# Operational Runbook: Dead-Letter Topic (DLT) Inspection & Replay

**Runbook ID:** RB-OPS-008  
**Target:** Kafka DLT Topics (`booking-events.DLT`, `payment-events.DLT`, `notification-events.DLT`)  
**Severity:** P2 (Poison Pill Isolation / Event Processing Recovery)

---

## 1. Symptoms
- Prometheus alert: `DeadLetterTopicEventDetected` or `KafkaDLTGrowthRate` firing.
- Consumer error handler exhausts retries (default 3 attempts) and routes unprocessable message to `.DLT` topic.
- Certain bookings or notifications are not fulfilled despite main topic progression.
- Microservice logs show `SeekToCurrentErrorHandler` or `DeadLetterPublishingRecoverer: Successfully routed record to DLT`.

---

## 2. Initial Checks
1. Identify which consumer group and topic triggered the DLT routing.
2. Verify why the consumer rejected the record:
   - Deserialization error (schema mismatch, invalid JSON).
   - Business validation exception (e.g. user not found, booking expired).
   - Downstream service unavailable.
3. Check DLT message count to determine if this is an isolated record or a systemic failure.

---

## 3. Commands

### Local (Docker Compose):
```bash
# Check message count in booking DLT topic
docker compose exec kafka kafka-run-class kafka.tools.GetOffsetShell \
  --bootstrap-server localhost:9092 --topic booking-events.DLT --time -1

# Check message count in payment DLT topic
docker compose exec kafka kafka-run-class kafka.tools.GetOffsetShell \
  --bootstrap-server localhost:9092 --topic payment-events.DLT --time -1

# Inspect and consume the dead-lettered messages with headers
docker compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic booking-events.DLT \
  --from-beginning \
  --property print.key=true \
  --property print.headers=true \
  --property print.timestamp=true \
  --max-messages 5
```

### Kubernetes:
```bash
# Inspect DLT messages from within kafka-0 pod
kubectl exec -it kafka-0 -n vibecheck -- kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic booking-events.DLT \
  --from-beginning \
  --property print.headers=true \
  --max-messages 5
```

---

## 4. Logs to Inspect
```bash
# Docker Compose: check consumer services for DLT recovery logs
docker compose logs --tail=300 vibecheck-notification | grep -iE "DeadLetterPublishingRecoverer|dlt|Record routed to"
docker compose logs --tail=300 vibecheck-booking | grep -iE "DeadLetterPublishingRecoverer|dlt|Record routed to"
```
Look for Kafka exception headers in the DLT message:
- `kafka_dlt-exception-fqcn` (Fully Qualified Class Name of the exception)
- `kafka_dlt-exception-message` (Root cause exception message)
- `kafka_dlt-original-topic` (Original source topic)
- `kafka_dlt-original-partition` (Original partition number)
- `kafka_dlt-original-offset` (Original offset)

---

## 5. Metrics to Inspect
- `vibecheck_kafka_dlt_events_total{topic="booking-events.DLT"}`
- `vibecheck_kafka_dlt_events_total{topic="payment-events.DLT"}`
- `kafka_consumergroup_lag{topic=~".*DLT"}`

---

## 6. Safe Recovery Procedure

### Scenario A: Transient Infrastructure Failure (Now Resolved)
If events were dead-lettered because a downstream service (e.g. SMTP server) was temporarily unreachable:
1. Replay messages from DLT back into the primary topic using Kafka console tools:
   ```bash
   # Read from DLT and pipe directly into the main topic
   docker compose exec kafka kafka-console-consumer \
     --bootstrap-server localhost:9092 \
     --topic booking-events.DLT \
     --from-beginning \
     --timeout-ms 5000 | \
   docker compose exec -T kafka kafka-console-producer \
     --bootstrap-server localhost:9092 \
     --topic booking-events
   ```
2. Monitor consumer group lag to ensure messages are successfully processed by active consumers.

### Scenario B: Schema or Deserialization Incompatibility
If an updated service produced an incompatible schema:
1. Do not replay immediately. Replaying will cause repeated consumer failure loops.
2. Deploy a hotfix to the consumer service containing the updated DTO / deserializer.
3. Once the updated consumer pod is running and healthy, replay the DLT messages as described in Scenario A.

### Scenario C: Corrupted / Bogus Message
If the message contains corrupted, nonsensical, or malicious data:
1. Document the message payload and Kafka headers in the incident postmortem report.
2. Do not replay. Allow the message to age out according to the DLT topic retention policy (default 14 days).

---

## 7. Verification
1. Verify the consumer processed the replayed message:
   ```bash
   docker compose logs --tail=50 vibecheck-notification | grep -i "Successfully processed event"
   ```
2. Verify no new messages landed back in `.DLT`:
   ```bash
   docker compose exec kafka kafka-run-class kafka.tools.GetOffsetShell \
     --bootstrap-server localhost:9092 --topic booking-events.DLT --time -1
   ```

---

## 8. Rollback Procedure
If replaying DLT messages causes unexpected consumer crashes:
- Immediately stop the consumer service: `docker compose stop vibecheck-notification`
- Reset consumer group offset past the problematic message or roll back the replayed messages.

---

## 9. Escalation Conditions
- DLT message volume exceeds 100 messages within 1 hour.
- Core booking confirmation events are trapped in DLT, impacting user tickets.
- Replayed messages continuously crash consumers upon consumption.
