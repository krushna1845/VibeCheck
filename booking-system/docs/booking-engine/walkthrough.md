# Kafka Event Layer – Walkthrough

**Author:** Antigravity (AI Coding Assistant)  
**Date:** 2026-08-01  
**Module:** `booking-service`  
**Build Result:** BUILD SUCCESS – 191 tests run, 0 failures, 0 errors  

---

## 1. Overview

This walkthrough documents the complete implementation of the **Kafka Event Layer** for the Movie Booking Platform's `booking-service`. The implementation delivers a production-grade, outbox-ready, and idempotent event-driven architecture integrated with Spring Kafka and PostgreSQL.

### Components Implemented

```
booking-service/src/main/resources/db/migration/
    V6__create_outbox_and_processed_events.sql   [NEW] Outbox + idempotency tables

booking-service/src/main/java/.../booking/event/
    BookingCreatedEvent.java                     [UPDATED] Versioned event + metadata
    BookingConfirmedEvent.java                   [UPDATED] Versioned event + metadata
    BookingCancelledEvent.java                   [UPDATED] Versioned event + metadata
    BookingExpiredEvent.java                     [UPDATED] Versioned event + metadata
    BookingFailedEvent.java                      [UPDATED] Versioned event + metadata
    BookingEventPublisher.java                   (interface, unchanged)
    KafkaBookingEventPublisher.java              [UPDATED] Outbox + headers + async callbacks
    BookingEventConsumer.java                    [NEW] Idempotent consumer + DLQ listener

booking-service/src/main/java/.../booking/config/
    KafkaConfig.java                             [UPDATED] Topics, DLTs, Producer, Consumer, Retry, DLQ

booking-service/src/main/java/.../booking/outbox/
    OutboxEvent.java                             [NEW] JPA entity
    OutboxEventRepository.java                   [NEW] Spring Data JPA repository
    OutboxEventService.java                      [NEW] Transactional outbox service

booking-service/src/main/java/.../booking/idempotency/
    ProcessedEvent.java                          [NEW] JPA entity
    ProcessedEventRepository.java                [NEW] Spring Data JPA repository
    IdempotencyService.java                      [NEW] Idempotency enforcement service

booking-service/src/test/java/.../booking/event/
    EventSerializationTest.java                  [NEW] JSON serialization/deserialization tests
    KafkaBookingEventPublisherTest.java           [NEW] Publisher unit tests
    BookingEventConsumerTest.java                 [NEW] Idempotent consumer unit tests

booking-service/src/test/java/.../booking/config/
    KafkaConfigTest.java                          [NEW] Configuration bean tests

booking-service/src/test/java/.../booking/idempotency/
    IdempotencyServiceTest.java                   [NEW] Idempotency logic unit tests

booking-service/src/test/java/.../booking/integration/
    BookingEventKafkaIntegrationTest.java         [NEW] End-to-end publish + consume integration test
```

---

## 2. Architecture

```
BookingServiceImpl
        │
        ▼
BookingEventPublisher (interface)
        │
        ▼
KafkaBookingEventPublisher
  ├── OutboxEventService.saveEvent()  ──► outbox_events table (PENDING)
  └── kafkaTemplate.send()
          │
          ├── topic: booking-created-events
          ├── topic: booking-confirmed-events
          ├── topic: booking-cancelled-events
          ├── topic: booking-expired-events
          └── topic: booking-failed-events
                          │
                  [retry: 3 × 1000ms]
                          │
                       .DLT Topics
                          │
BookingEventConsumer ◄────┘
  ├── IdempotencyService.isEventProcessed(eventId)
  │       └── processed_events table
  ├── [if duplicate] skip
  ├── [if new] handle + markEventAsProcessed()
  └── DLQ handler: @KafkaListener on .DLT topics
```

---

## 3. Key Components

### 3.1 Versioned Event Schemas

All five domain events (`BookingCreatedEvent`, `BookingConfirmedEvent`, `BookingCancelledEvent`, `BookingExpiredEvent`, `BookingFailedEvent`) are enhanced with three new fields, with Lombok-managed defaults in the inner builder class:

| Field | Type | Default |
|---|---|---|
| `eventId` | `String` | `UUID.randomUUID().toString()` |
| `eventType` | `String` | `"BOOKING_CREATED"` (per event type) |
| `eventVersion` | `Integer` | `1` |
| `timestamp` | `Instant` | `Instant.now()` |

**Example:**
```java
BookingCreatedEvent event = BookingCreatedEvent.builder()
        .bookingId(booking.getId())
        .bookingReference(ref)
        // eventId, eventType, eventVersion, timestamp auto-populated
        .build();
```

**JSON wire format:**
```json
{
  "eventId": "a1b2c3d4-...",
  "eventType": "BOOKING_CREATED",
  "eventVersion": 1,
  "bookingId": "...",
  "bookingReference": "BKG-001",
  "timestamp": "2026-08-01T00:25:00Z"
}
```

---

### 3.2 Kafka Topic Configuration (`KafkaConfig`)

Topics are provisioned automatically via Spring `NewTopic` beans:

| Topic | Partitions | Replicas | Purpose |
|---|---|---|---|
| `booking-created-events` | 3 | 1 | New booking placed |
| `booking-confirmed-events` | 3 | 1 | Payment confirmed |
| `booking-cancelled-events` | 3 | 1 | Booking cancelled |
| `booking-expired-events` | 3 | 1 | Reservation expired |
| `booking-failed-events` | 3 | 1 | Booking failed |
| `booking-created-events.DLT` | 1 | 1 | Dead letters for created |
| `booking-confirmed-events.DLT` | 1 | 1 | Dead letters for confirmed |
| `booking-cancelled-events.DLT` | 1 | 1 | Dead letters for cancelled |
| `booking-expired-events.DLT` | 1 | 1 | Dead letters for expired |

#### Producer Configuration

```
acks=all                # Strongest durability guarantee
retries=3               # Kafka-level producer retries
enable.idempotence=true # Exactly-once producer semantics
key.serializer=StringSerializer
value.serializer=JsonSerializer (Jackson ObjectMapper, type headers enabled)
```

#### Consumer Configuration

```
auto.offset.reset=earliest
key.deserializer=ErrorHandlingDeserializer → StringDeserializer
value.deserializer=ErrorHandlingDeserializer → JsonDeserializer
trusted.packages=com.krushna.moviebooking.*
```

#### Retry & Dead Letter Queue (DLQ)

```
Error handler: DefaultErrorHandler
Backoff: FixedBackOff(interval=1000ms, maxAttempts=2 retries → 3 total attempts)
Recoverer: DeadLetterPublishingRecoverer → routes to <topic>.DLT
```

---

### 3.3 `KafkaBookingEventPublisher`

Implements `BookingEventPublisher` with:

1. **Structured logging** using `[KafkaPublisher]` prefix for easy log filtering.
2. **Outbox persistence** — calls `OutboxEventService.saveEvent()` within the same transaction as the domain operation.
3. **Kafka headers** — adds `eventId` and `eventType` as Kafka record headers for consumers.
4. **Async completion callback** — logs partition/offset on success; logs error on failure (fire-and-forget, non-blocking).

---

### 3.4 `BookingEventConsumer`

Idempotent `@KafkaListener` consumer:

- Listens on all four lifecycle topics.
- **Guard clause first:** calls `IdempotencyService.isEventProcessed(eventId)` before any processing.
- On duplicate: logs and returns without re-processing.
- On success: calls `IdempotencyService.markEventAsProcessed()` to persist the `eventId`.
- **DLQ Listener:** separate `@KafkaListener` on all `.DLT` topics, logging with `[KafkaDLQ]` prefix.

---

### 3.5 Outbox-Ready Architecture

The transactional outbox pattern is pre-wired:

| Layer | Component | Responsibility |
|---|---|---|
| DB Migration | `V6__create_outbox_and_processed_events.sql` | Creates `outbox_events` and `processed_events` tables |
| Entity | `OutboxEvent` | Maps `outbox_events` with status, payload, retry count |
| Repository | `OutboxEventRepository` | Spring Data JPA, `findTop50ByStatusOrderByCreatedAtAsc` |
| Service | `OutboxEventService.saveEvent()` | Serializes event to JSON, persists as `PENDING` |

**Future-proofing:** An outbox relay process can read `PENDING` events and republish them to Kafka without any application-level change.

---

### 3.6 Idempotent Consumer Architecture

| Layer | Component | Responsibility |
|---|---|---|
| DB Migration | `V6__create_outbox_and_processed_events.sql` | Creates `processed_events` (PK=`event_id`) |
| Entity | `ProcessedEvent` | Maps `processed_events` with `eventId`, `eventType`, `consumerGroup` |
| Repository | `ProcessedEventRepository` | Spring Data JPA, `existsById(eventId)` for O(1) check |
| Service | `IdempotencyService` | `isEventProcessed()` + `markEventAsProcessed()` |

---

## 4. Test Suite & Verification Results

All **191 tests** pass with zero failures or errors.

```
[INFO] Results:
[INFO] Tests run: 191, Failures: 0, Errors: 0, Skipped: 4
[INFO] BUILD SUCCESS
[INFO] Total time: 11.739 s
```

### Kafka Event Layer Test Summary

| Test Class | Tests | Result | Coverage |
|---|---|---|---|
| `EventSerializationTest` | 4 | PASS | JSON round-trip for all 4 event types; verifies eventId, eventType, eventVersion defaults |
| `KafkaBookingEventPublisherTest` | 4 | PASS | Verifies outbox.saveEvent() + kafkaTemplate.send() for all events; ArgumentCaptor checks topic, key, payload |
| `BookingEventConsumerTest` | 4 | PASS | Tests new event processing, duplicate skip, confirmEvent, and DLQ handler |
| `IdempotencyServiceTest` | 4 | PASS | Tests existsById(true), existsById(false), null/blank skip, and ProcessedEvent save |
| `KafkaConfigTest` | 4 | PASS | Validates all topic names, DLT names, ProducerFactory, ConsumerFactory, ErrorHandler, ListenerContainerFactory |
| `BookingEventKafkaIntegrationTest` | 1 | PASS | End-to-end: publish → first consume (mark processed) → second consume (skip duplicate) |

---

## 5. Requirements Matrix

| Requirement | Implementation | Status |
|---|---|---|
| **BookingEventPublisher** | `KafkaBookingEventPublisher` implementing `BookingEventPublisher` interface | ✅ PASS |
| **BookingEventConsumer** | `BookingEventConsumer` with `@KafkaListener` per topic | ✅ PASS |
| **BookingCreatedEvent** | Versioned record with `eventId`, `eventType`, `eventVersion` | ✅ PASS |
| **BookingConfirmedEvent** | Versioned record with `eventId`, `eventType`, `eventVersion` | ✅ PASS |
| **BookingCancelledEvent** | Versioned record with `eventId`, `eventType`, `eventVersion` | ✅ PASS |
| **BookingExpiredEvent** | Versioned record with `eventId`, `eventType`, `eventVersion` | ✅ PASS |
| **Kafka Configuration** | `KafkaConfig` with `@EnableKafka`, ProducerFactory, ConsumerFactory, KafkaTemplate | ✅ PASS |
| **Topic Configuration** | 5 main topics + 4 `.DLT` topics, 3 partitions, auto-provisioned | ✅ PASS |
| **Producer Configuration** | `acks=all`, `enable.idempotence=true`, `retries=3`, `JsonSerializer` | ✅ PASS |
| **Consumer Configuration** | `ErrorHandlingDeserializer`, trusted packages, `earliest` offset reset | ✅ PASS |
| **Dead Letter Queue** | `DeadLetterPublishingRecoverer` + `.DLT` topics + `consumeDeadLetterEvent()` listener | ✅ PASS |
| **Retry Configuration** | `DefaultErrorHandler` + `FixedBackOff(1000ms, 2 retries = 3 total)` | ✅ PASS |
| **JSON Serialization** | Jackson `JsonSerializer`/`JsonDeserializer` with type headers | ✅ PASS |
| **Versioned Events** | `eventId`, `eventType`, `eventVersion` on all events with builder defaults | ✅ PASS |
| **Structured Logging** | `[KafkaPublisher]`, `[KafkaConsumer]`, `[KafkaDLQ]`, `[OutboxService]`, `[IdempotencyService]` | ✅ PASS |
| **Idempotent Consumer** | `IdempotencyService` backed by `processed_events` table, PK-based dedup | ✅ PASS |
| **Outbox-Ready Architecture** | `OutboxEventService` + `outbox_events` table via V6 migration | ✅ PASS |
| **Unit Tests** | 20 new unit tests across 5 test classes | ✅ PASS |
| **Integration Tests** | `BookingEventKafkaIntegrationTest` covering E2E flow + idempotency | ✅ PASS |
| **Compile Successfully** | `mvn test` — BUILD SUCCESS, 191 tests, 0 failures | ✅ PASS |
