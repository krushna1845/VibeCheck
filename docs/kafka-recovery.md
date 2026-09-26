# Kafka Recovery — VibeCheck Platform

**Document Version:** 1.0  
**Milestone:** 31 — Production Operations & Disaster Recovery  
**Date:** 2026-09-26

---

## 1. Current Kafka Configuration

### 1.1 Docker Compose (Local Development)

```yaml
kafka:
  image: confluentinc/cp-kafka:7.5.0
  container_name: vibecheck-kafka
  restart: unless-stopped
  environment:
    KAFKA_BROKER_ID: 1
    KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    KAFKA_AUTO_CREATE_TOPICS_ENABLE: 'true'
  volumes:
    - kafka_data:/var/lib/kafka/data
```

### 1.2 Kubernetes (Helm — StatefulSet)

```yaml
# From kafka-statefulset.yaml
command: "rm -f /var/lib/kafka/data/meta.properties && unset KAFKA_PORT && /etc/confluent/docker/run"
volumeMounts:
  - name: kafka-data
    mountPath: /var/lib/kafka/data
volumeClaimTemplates:
  - size: 5Gi
    accessMode: ReadWriteOnce
```

### 1.3 Topology Assessment

| Aspect | Current Configuration | Production Recommendation |
|---|---|---|
| Broker count | **1** (single broker) | 3+ brokers for HA |
| Replication factor | **1** (no replication) | 3 (min 2 in-sync replicas) |
| Zookeeper | Single instance | 3+ instance ensemble |
| Log retention | Confluent default (7 days) | Explicitly configured |
| Data persistence | Docker named volume / K8s PVC | Cloud-persistent volume |
| Message ordering | Per-partition (single broker = total) | Per-partition (multi-broker) |

> ⚠️ **Important:** This is a **single-broker, single-Zookeeper** development topology.  
> It does NOT provide High Availability. A single broker failure = complete Kafka unavailability.  
> Do NOT claim multi-broker resilience for this environment.

---

## 2. Topics and Consumer Groups

### 2.1 Known Topics (Auto-Created)

| Topic | Producer | Consumer | Purpose |
|---|---|---|---|
| `booking-events` | booking-service | notification-service | BookingConfirmedEvent, BookingCancelledEvent, BookingExpiredEvent |
| `booking-events.DLT` | Kafka infrastructure | Manual / DLT processor | Dead-letter topic for failed events |
| `payment-events` | payment-service | booking-service | PaymentSuccessEvent, PaymentFailedEvent |
| Notification topics | Various | notification-service | Email/SMS notification dispatch |

### 2.2 Consumer Groups

| Group | Service | Auto-Offset-Reset |
|---|---|---|
| `notification-service-group` | notification-service | `earliest` |
| `booking-service-group` | booking-service | Defaults per listener |
| `payment-service-group` | payment-service | Per listener config |

---

## 3. Event Durability: Transactional Outbox Pattern

### 3.1 How Events Are Durably Produced

VibeCheck uses the **Transactional Outbox Pattern** to guarantee event delivery:

1. When a booking is confirmed, the booking service writes to both:
   - `bookings` table (MySQL, in the same transaction)
   - `outbox_events` table (MySQL, in the same transaction)
2. The **OutboxRelayScheduler** periodically polls `outbox_events` WHERE `status = 'PENDING'`.
3. The scheduler publishes the event to Kafka.
4. On successful publish, the event is marked `status = 'PROCESSED'`.
5. On failure, retry count increments and `next_retry_at` is set (exponential backoff).
6. Events that exceed max retries are marked `status = 'FAILED'` (dead-letter in MySQL).

### 3.2 Outbox Recovery After Kafka Restart

**Kafka restarts but outbox events were not published:**

1. OutboxRelayScheduler runs on its next scheduled interval (typically every few seconds).
2. It picks up PENDING events from MySQL (which survived Kafka downtime).
3. Events are published to Kafka once the broker is available.
4. **No events are lost** — they remain durable in MySQL.

This is the key resilience guarantee: **Kafka loss does not cause booking event loss** as long as MySQL is intact.

---

## 4. Broker Failure Behavior

### 4.1 Kafka Container Crash (Volume Intact)

**What happens:**
1. Kafka broker stops.
2. `restart: unless-stopped` restarts the container.
3. Kafka reads segment files from `kafka_data` volume.
4. Consumer offsets (stored in `__consumer_offsets` topic on the broker) are restored.
5. Consumers reconnect and resume from last committed offset.
6. Producers reconnect (Spring Kafka auto-reconnects).
7. OutboxRelayScheduler: retries publishing on next interval after broker is ready.

**Data loss:** None (single broker, but all state is on the persistent volume).  
**RTO:** 60–120 seconds (Zookeeper re-election + Kafka startup).

### 4.2 Kafka Volume Loss

**What happens:**
1. Kafka starts with empty state (no topics, no offsets, no segment data).
2. Topics are auto-created on first producer message (`KAFKA_AUTO_CREATE_TOPICS_ENABLE: true`).
3. Consumer groups start from `auto.offset.reset: earliest` (consuming from beginning of new empty topics — no events to consume).
4. **Historical events are lost** from Kafka — but the Outbox relay will republish PENDING events.
5. Events already marked PROCESSED in MySQL will NOT be re-published (relay only sends PENDING).

**Impact assessment:**
- Events PENDING in outbox → Re-published by relay after Kafka recovery ✅
- Events PROCESSED in MySQL (already delivered to consumers) → NOT re-published (by design) ✅
- Events in-flight (published but consumer not yet committed offset) → Lost (consumer will not see them, but they are PROCESSED in outbox) ⚠️
  - Mitigation: Kafka consumer idempotency via `processed_events` table means consumers skip already-processed events.

### 4.3 Zookeeper Failure

**What happens:**
1. Kafka loses coordination (leader election unavailable).
2. Kafka broker becomes unavailable for producing/consuming.
3. When Zookeeper recovers, Kafka reconnects within ~30 seconds.
4. No data loss (Kafka log files are independent of Zookeeper).

**RTO:** 60–90 seconds (Zookeeper restart + Kafka reconnect).

### 4.4 Network Partition (Single Broker — Not Applicable)

Single-broker topology has no network partition between brokers.  
Network partition between application and Kafka broker = Kafka unavailable until network recovers.

---

## 5. Consumer Offset Behavior

### 5.1 Normal Operation

Consumer offsets are committed to Kafka's `__consumer_offsets` topic after each batch.  
Spring Kafka uses `ack-mode: BATCH` by default — offsets committed after batch processing.

**At-least-once delivery is guaranteed:**
- If consumer commits but processing fails → event is lost (not reprocessed unless reset).
- If consumer processes but doesn't commit → event is reprocessed on restart.

VibeCheck's `processed_events` table in MySQL provides **consumer-side idempotency** to handle at-least-once redelivery safely.

### 5.2 After Kafka Restart

1. Consumers reconnect to broker.
2. Kafka returns last committed offset for each partition.
3. Consumers resume from last committed offset.
4. **No events are skipped** (offset is persisted on broker volume).
5. **No events are re-processed** (offset is persisted).

### 5.3 After Consumer Service Restart

1. Consumer service restarts and rejoins consumer group.
2. Consumer group rebalance triggers (may take 30–60 seconds for notification-service-group).
3. Partition assignments redistributed among healthy consumers.
4. Processing resumes from last committed offset.

---

## 6. DLT Recovery

### 6.1 Dead-Letter Topic Overview

Failed Kafka consumer message processing routes to `*.DLT` topics.  
The DLT provides a durability net for messages that fail consumer-side processing.

### 6.2 DLT Recovery Procedure

```powershell
# Step 1: Inspect DLT messages
docker exec vibecheck-kafka kafka-console-consumer `
  --bootstrap-server localhost:9092 `
  --topic booking-events.DLT `
  --from-beginning `
  --max-messages 10

# Step 2: Identify root cause
# Check consumer logs for the service consuming the original topic
docker logs vibecheck-notification --tail 100 | Select-String "DLT|DeadLetter|error"

# Step 3: Fix root cause (code fix, data fix, dependency recovery)

# Step 4: If events should be re-processed, reset the original consumer group offset
# WARNING: This will cause reprocessing from the specified offset
docker exec vibecheck-kafka kafka-consumer-groups `
  --bootstrap-server localhost:9092 `
  --group notification-service-group `
  --topic booking-events `
  --reset-offsets `
  --to-earliest `
  --execute

# Step 5: Monitor that events are processed after offset reset
docker logs vibecheck-notification --tail 50 --follow
```

---

## 7. Recovery Procedures

### 7.1 Kafka Restart (Container Recovery)

```powershell
# Check Kafka status
docker compose ps kafka

# Restart Kafka (volume preserved)
docker compose restart kafka

# Wait for Kafka to be healthy
$maxWait = 120
$elapsed = 0
do {
    Start-Sleep -Seconds 5
    $elapsed += 5
    $status = docker inspect --format '{{.State.Health.Status}}' vibecheck-kafka 2>$null
    Write-Host "[$elapsed s] Kafka health: $status"
} while ($status -ne "healthy" -and $elapsed -lt $maxWait)

# Verify consumer groups reconnected
docker exec vibecheck-kafka kafka-consumer-groups `
  --bootstrap-server localhost:9092 `
  --list

# Check outbox relay is re-publishing pending events
docker logs vibecheck-booking --tail 30 | Select-String "outbox|relay|publish"
```

### 7.2 Full Kafka + Zookeeper Restart

```powershell
# Stop Kafka and Zookeeper
docker compose stop kafka zookeeper

# Restart in dependency order
docker compose up -d zookeeper
Start-Sleep -Seconds 20
docker compose up -d kafka

# Verify health
docker compose ps kafka zookeeper

# Verify topics are available
docker exec vibecheck-kafka kafka-topics `
  --bootstrap-server localhost:9092 `
  --list
```

### 7.3 Kubernetes Kafka Recovery

```bash
# Check Kafka StatefulSet
kubectl get statefulset kafka -n vibecheck

# Restart Kafka pod (PVC preserved)
kubectl delete pod kafka-0 -n vibecheck

# Monitor restart
kubectl get pods -n vibecheck -w

# Verify Kafka is accessible from within cluster
kubectl exec -n vibecheck kafka-0 -- \
  kafka-broker-api-versions --bootstrap-server localhost:9092
```

---

## 8. Limitations of Current Topology

| Limitation | Impact | Mitigation |
|---|---|---|
| Single Kafka broker | Complete Kafka unavailability on broker failure | Outbox pattern compensates during downtime |
| Replication factor = 1 | Message loss if broker volume fails before being consumed | Transactional outbox provides durability |
| No Kafka backup | Topic history lost on volume deletion | Outbox PENDING events can be re-published |
| No multi-broker consumer balancing | No partition distribution across brokers | Irrelevant for single-broker |
| Single Zookeeper | Zookeeper failure = Kafka unavailable | Not mitigated in current architecture |
| No Kafka Connect | No schema registry | Events use JSON without schema enforcement |
| No lag alerting (configured) | Consumer lag not alerted | Metrics exported to Prometheus; alerting not configured |
