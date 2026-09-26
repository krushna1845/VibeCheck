# Operational Runbook: Transactional Outbox Backlog & Relay Recovery

**Runbook ID:** RB-OPS-007  
**Target:** `booking-service` Outbox Relay, `payment-service` Outbox Relay  
**Severity:** P1 (Asynchronous Event Delivery Stalled)

---

## 1. Symptoms
- Prometheus alert: `OutboxPendingBacklog` or `OutboxRelayFailureRate` firing.
- `outbox_events` table count growing continuously in `vibecheck_booking` or `vibecheck_payment`.
- Downstream services (notification-service, booking-service) do not receive event updates.
- Microservice logs show repeated messages: `Failed to publish outbox event id=...`, `Kafka producer timeout`, or `Retrying outbox event delivery`.

---

## 2. Initial Checks
1. Check Kafka broker availability and network reachability from microservices.
2. Check `status` distribution in the `outbox_events` table (`PENDING`, `IN_FLIGHT`, `PUBLISHED`, `FAILED`).
3. Check whether the Outbox Relay scheduled task is running or blocked on a single poison pill message.
4. Check database query performance on `outbox_events` (missing index on `status` or `created_at`).

---

## 3. Commands

### Local (Docker Compose):
```bash
# Check count of pending and failed outbox events in booking database
docker compose exec mysql mysql -uroot -p"$DB_ROOT_PASSWORD" -e "
  SELECT status, count(*), MIN(created_at) as oldest_event 
  FROM vibecheck_booking.outbox_events GROUP BY status;"

# Check oldest stuck pending outbox records
docker compose exec mysql mysql -uroot -p"$DB_ROOT_PASSWORD" -e "
  SELECT id, aggregate_type, aggregate_id, event_type, retry_count, last_error, created_at 
  FROM vibecheck_booking.outbox_events 
  WHERE status IN ('PENDING', 'FAILED') ORDER BY created_at ASC LIMIT 10;"

# Check payment service outbox table
docker compose exec mysql mysql -uroot -p"$DB_ROOT_PASSWORD" -e "
  SELECT status, count(*) FROM vibecheck_payment.outbox_events GROUP BY status;"
```

### Kubernetes:
```bash
# Query outbox metrics via Prometheus query
kubectl exec -it deployment/prometheus -n vibecheck -- \
  wget -qO- "http://localhost:9090/api/v1/query?query=vibecheck_outbox_pending_count"
```

---

## 4. Logs to Inspect
```bash
# Docker Compose: search for outbox relay worker logs
docker compose logs --tail=300 vibecheck-booking | grep -iE "OutboxRelay|outbox|Publishing outbox event|Failed to send outbox"

# Check Kafka producer exceptions
docker compose logs --tail=200 vibecheck-booking | grep -iE "TimeoutException|RetriableException|ProducerFencedException"
```

---

## 5. Metrics to Inspect
- `vibecheck_outbox_pending_count`
- `vibecheck_outbox_published_total`
- `vibecheck_outbox_failed_total`
- `vibecheck_outbox_relay_duration_seconds`
- `kafka_producer_record_error_total`

---

## 6. Safe Recovery Procedure

### Scenario A: Kafka Broker Was Down and Just Recovered
Once Kafka broker is healthy (`vibecheck-kafka` up and responding on port 9092):
1. The scheduled outbox relay job (`@Scheduled(fixedDelay = 2000)`) will automatically resume polling `PENDING` records.
2. Monitor backlog drainage:
   ```bash
   watch -n 2 'docker compose exec mysql mysql -uroot -p"$DB_ROOT_PASSWORD" -e "SELECT status, count(*) FROM vibecheck_booking.outbox_events GROUP BY status;"'
   ```
3. Backlog should decrease to 0 within 1–3 minutes depending on batch size.

### Scenario B: Stuck In-Flight Records
If a previous pod crash left records marked as `PROCESSING` or `IN_FLIGHT`:
1. Reset records older than 5 minutes back to `PENDING`:
   ```bash
   docker compose exec mysql mysql -uroot -p"$DB_ROOT_PASSWORD" -e "
     UPDATE vibecheck_booking.outbox_events 
     SET status = 'PENDING', updated_at = NOW() 
     WHERE status = 'IN_FLIGHT' AND updated_at < NOW() - INTERVAL 5 MINUTE;"
   ```
2. The relay worker will pick them up on the next polling cycle.

### Scenario C: Poison Pill Record (Malformed Payload)
If a specific event fails repeatedly and exceeds max retries (`retry_count >= 5`):
1. Quarantine the poison pill by moving it to `DEAD_LETTER`:
   ```bash
   docker compose exec mysql mysql -uroot -p"$DB_ROOT_PASSWORD" -e "
     UPDATE vibecheck_booking.outbox_events 
     SET status = 'DEAD_LETTER', last_error = 'Quarantined by SRE runbook' 
     WHERE status = 'FAILED' AND retry_count >= 5;"
   ```
2. The outbox relay worker will unblock and process subsequent valid events.
3. Investigate the quarantined payload offline for data integrity bugs.

---

## 7. Verification
1. Verify pending outbox count drops to 0 or near-0:
   ```bash
   docker compose exec mysql mysql -uroot -p"$DB_ROOT_PASSWORD" -e "
     SELECT count(*) as pending_count FROM vibecheck_booking.outbox_events WHERE status = 'PENDING';"
   ```
2. Check that target Kafka topic received published messages:
   ```bash
   docker compose exec kafka kafka-run-class kafka.tools.GetOffsetShell --bootstrap-server localhost:9092 --topic booking-events --time -1
   ```

---

## 8. Rollback Procedure
If database manual queries caused incorrect state updates:
- Review `last_error` and `updated_at` timestamps in `outbox_events`.
- Revert statuses with timestamp filter:
  `UPDATE vibecheck_booking.outbox_events SET status = 'PENDING' WHERE status = 'DEAD_LETTER' AND last_error LIKE 'Quarantined%';`

---

## 9. Escalation Conditions
- Backlog exceeds 5,000 records and continues growing.
- Outbox relay throws continuous SQL deadlocks during batch selection.
- Events fail to publish even when Kafka console producer works normally.
