# Transactional Outbox Delivery Semantics & Architecture Report

This document details the architectural design, concurrency controls, and failure recovery mechanics implemented for the hardened **Transactional Outbox** system within the Movie Booking Application.

---

## 1. Core Objectives & Design Goals

*   **Atomic Database State & Outbox Persistence**: Guarantee that any booking state transition (creation, confirmation, cancellation, expiration) is saved atomically with its corresponding outbox event.
*   **Asynchronous decoupled Kafka dispatching**: Ensure no business transactions depend on external message broker availability. Kafka network hops are eliminated from the write path.
*   **Multi-instance Safe Concurrency**: Provide a highly-scalable, non-blocking lock model allowing multiple outbox scheduler instances to claim events concurrently without duplicate delivery or row contention.
*   **Resiliency & Automatic Recovery**: Handle transient broker failures using bounded exponential retry backoffs, moving persistently failing events to a Dead Letter Queue (DLQ) after retry exhaustion.
*   **At-Least-Once Delivery & Consumer Idempotency**: Establish a complete end-to-end guarantee that messages are delivered at least once, with deduplication at the consumer boundaries.

---

## 2. Architectural Blueprint

The outbox implementation replaces direct message publishing inside services with a write-ahead logging pattern inside the database transaction boundary:

```mermaid
sequenceDiagram
    autonumber
    actor Client
    participant BookingService as Booking Service
    database DB as Booking DB
    participant Relay as Outbox Relay Scheduler
    participant Kafka as Kafka Broker
    participant Consumer as Booking Consumer

    Client->>BookingService: Create Booking Request
    Note over BookingService: Begin Transaction
    BookingService->>DB: Save Booking (PENDING)
    BookingService->>DB: Save OutboxEvent (PENDING)
    Note over BookingService: Commit Transaction
    DB-->>BookingService: Transaction Committed Success
    BookingService-->>Client: Booking Created response (201)

    Note over Relay: Run Cron Loop (e.g. every 1s)
    Relay->>DB: claimEventsForProcessing() (Propagation.REQUIRES_NEW)
    Note over DB: SELECT FOR UPDATE SKIP LOCKED
    DB-->>Relay: Return claimed events (IN_PROGRESS, lease: 60s)
    
    loop For each claimed event
        Relay->>Kafka: send(ProducerRecord)
        Kafka-->>Relay: ACK (RecordMetadata)
        Relay->>DB: markAsPublished(eventId)
    end
    
    Kafka->>Consumer: Deliver Event (At-least-once)
    Note over Consumer: check if eventId processed
    Consumer->>DB: Save ProcessedEvent (eventId)
    Note over Consumer: Process message business logic
```

---

## 3. Detailed Component Architecture

### A. Atomicity & Database Transaction Boundaries

All business state changes in [BookingServiceImpl](file:///c:/Users/acer/Downloads/booking-system/booking-system/booking-service/src/main/java/com/krushna/moviebooking/booking/service/impl/BookingServiceImpl.java) are marked with `@Transactional`.

The [KafkaBookingEventPublisher](file:///c:/Users/acer/Downloads/booking-system/booking-system/booking-service/src/main/java/com/krushna/moviebooking/booking/event/KafkaBookingEventPublisher.java) has been completely refactored to remove references to `KafkaTemplate`. Instead, it converts event objects into JSON using an `ObjectMapper` and delegates storage to [OutboxEventService](file:///c:/Users/acer/Downloads/booking-system/booking-system/booking-service/src/main/java/com/krushna/moviebooking/booking/outbox/OutboxEventService.java):

```java
// Inside OutboxEventService.java
@Transactional(propagation = Propagation.MANDATORY)
public OutboxEvent saveEvent(String aggregateType, String aggregateId, String eventType, int version, Object payload) {
    String payloadJson = objectMapper.writeValueAsString(payload);
    OutboxEvent event = OutboxEvent.builder()
            .aggregateType(aggregateType)
            .aggregateId(aggregateId)
            .eventType(eventType)
            .eventVersion(version)
            .payload(payloadJson)
            .status(OutboxEvent.STATUS_PENDING)
            .retryCount(0)
            .build();
    return outboxEventRepository.save(event);
}
```

Since propagation is set to `MANDATORY`, the event save operation participates directly in the caller's active database transaction. If the booking transaction fails and rolls back, the outbox event rolls back cleanly with it.

---

### B. High-Throughput Multi-Instance Concurrency (`SKIP LOCKED`)

To prevent multiple scheduler instances from retrieving the same pending events or blocking one another on row locks, the Outbox Relay claim phase uses a pessimistic lock with skip locked capabilities.

The database query in [OutboxEventRepository](file:///c:/Users/acer/Downloads/booking-system/booking-system/booking-service/src/main/java/com/krushna/moviebooking/booking/outbox/OutboxEventRepository.java) leverages Hibernate’s `PESSIMISTIC_WRITE` lock timeout hints to compile to native `FOR UPDATE SKIP LOCKED` DDL instructions:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@QueryHints({@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2")})
@Query("SELECT e FROM OutboxEvent e WHERE e.status = 'PENDING' OR " +
       "(e.status = 'FAILED' AND e.nextRetryAt <= :now) " +
       "ORDER BY e.createdAt ASC, e.id ASC")
List<OutboxEvent> findPendingOrRetryableEventsWithLock(Instant now, Pageable pageable);
```

#### The Claim Protocol:
1. Schedulers fetch batch sizing (e.g., 50 records) using `findPendingOrRetryableEventsWithLock`.
2. Any row locked by another scheduler instance is skipped immediately (`SKIP LOCKED`), preventing thread blockages and yielding maximum parallel processing throughput.
3. The claim transaction runs in `Propagation.REQUIRES_NEW`. It transitions the records to `IN_PROGRESS` with a 60-second lease timestamp (`leasedUntil = Instant.now() + 60s`) and commits immediately.
4. Rows are unlocked instantly, and the actual network dispatch to Kafka is performed outside of any database transaction, ensuring DB connection pools are never saturated by transient network delays.

---

### C. Exponential Backoff, Leases, and DLQ Handling

To handle transient network drops or broker downtime, events that fail to publish are updated with a retry delay using an exponential backoff formula:

$$\text{delay} = 2^{\text{retryCount}} \times 2\text{ seconds}$$

#### Failure State Transitions:
*   **PENDING / RETRYABLE**: The event is ready for processing.
*   **IN_PROGRESS**: The event is claimed. A lease expiration timestamp is set. If the worker crashes mid-publish, a reclamation background task finds stale events (`leasedUntil <= Instant.now()`) and resets them to `PENDING`.
*   **PUBLISHED**: Successfully acknowledged by the Kafka broker.
*   **FAILED**: A retryable failure occurred. The retry count is incremented, and `nextRetryAt` is calculated. The exact exception error trace is logged in `errorMessage` for observability.
*   **DEAD_LETTER**: If the retry count reaches `5`, the event status transitions to `DEAD_LETTER`. It is retained in the database for manual auditing and troubleshooting, ensuring zero message loss.

---

## 4. End-to-End Delivery Semantics

Due to network retry loops and consumer acknowledgment flows, message brokers generally offer at-least-once delivery. The outbox pattern guarantees that a message is successfully delivered to the broker *at least once*, which means duplicate deliveries can occur under network partitions.

To enforce exact-once processing semantics, the system applies **Consumer-Side Deduplication**:

1. Every event structure carries a unique `eventId` generated in the database before transaction commit.
2. The receiving microservice intercepts incoming messages in [BookingEventConsumer](file:///c:/Users/acer/Downloads/booking-system/booking-system/booking-service/src/main/java/com/krushna/moviebooking/booking/event/BookingEventConsumer.java).
3. The consumer executes [IdempotencyService](file:///c:/Users/acer/Downloads/booking-system/booking-system/booking-service/src/main/java/com/krushna/moviebooking/booking/idempotency/IdempotencyService.java) within a database transaction, persisting the `eventId` to the `processed_events` table before processing:

```java
@Transactional
public boolean checkAndMarkProcessed(String eventId) {
    if (processedEventRepository.existsById(eventId)) {
        log.warn("[Idempotency] Duplicate event detected. Skipping: {}", eventId);
        return false; // Already processed
    }
    processedEventRepository.save(new ProcessedEvent(eventId, Instant.now()));
    return true; // Safe to process
}
```

If a duplicate message is received, the consumer discards it immediately without invoking downstream business logic.

---

## 5. Architectural Verification Results

The test suite [TransactionalOutboxHardeningTest.java](file:///c:/Users/acer/Downloads/booking-system/booking-system/booking-service/src/test/java/com/krushna/moviebooking/booking/outbox/TransactionalOutboxHardeningTest.java) verifies every component of the architecture:

*   `transactionRollback_NoOutboxOrBookingCommitted`: Confirms that business transaction failures roll back both booking and outbox entries cleanly.
*   `transactionCommit_OutboxAndBookingPersistedAtomically`: Validates atomic state persistence.
*   `kafkaUnavailable_EventTransitionsToFailedWithBackoff`: Verifies transition to `FAILED` and calculation of exponential retry time.
*   `kafkaRecovery_EventPublishedSuccessfully`: Confirms retried messages transition to `PUBLISHED` upon broker recovery.
*   `retryExhaustion_TransitionsToDeadLetter`: Confirms `DEAD_LETTER` handling after maximum retry counts are reached.
*   `concurrentRelayWorkers_ClaimDisjointEvents`: Simulates concurrent scheduler instances claiming disjoint events without double processing.
*   `consumerDuplicateDelivery_SkippedSafely`: Assures that duplicated messages are intercepted by the idempotency layer.
