# Operational Runbook: Kafka Recovery & Troubleshooting

**Runbook ID:** RB-OPS-004  
**Target:** Apache Kafka & Zookeeper (`vibecheck-kafka`, `vibecheck-zookeeper`)  
**Severity:** P1 (Event Pipeline / Asynchronous Order Processing Impaired)

---

## 1. Symptoms
- Prometheus alert: `KafkaBrokerDown` or `KafkaConsumerLagCritical` firing.
- Booking service outbox relay reports `Failed to send message to Kafka topic: booking-events`.
- Notification service stops sending booking confirmation emails/SMS.
- Payment completed events are not consumed by booking-service to finalize booking states.
- Microservice logs filled with `DisconnectException`, `TimeoutException: Expiring 1 record(s)`, or `NetworkException`.

---

## 2. Initial Checks
1. Check Zookeeper health first (Kafka depends on Zookeeper in this topology).
2. Check Kafka broker container status and disk usage in `/var/lib/kafka/data`.
3. Check whether consumer group offsets are committing or lagging.
4. Verify whether transactional outbox table (`outbox_events`) is accumulating pending records.

---

## 3. Commands

### Local (Docker Compose):
```bash
# Check Zookeeper and Kafka container status
docker compose ps zookeeper kafka

# Check Zookeeper ruok 4-letter word
docker compose exec zookeeper sh -c 'echo "ruok" | nc localhost 2181'
# Should output: imok

# List active Kafka topics
docker compose exec kafka kafka-topics --bootstrap-server localhost:9092 --list

# Check consumer group lag (e.g. notification-group)
docker compose exec kafka kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group notification-group
```

### Kubernetes:
```bash
# Check StatefulSets and pods
kubectl get statefulset kafka -n vibecheck
kubectl get pod kafka-0 -n vibecheck
kubectl get deployment zookeeper -n vibecheck

# List topics inside kafka pod
kubectl exec -it kafka-0 -n vibecheck -- kafka-topics --bootstrap-server localhost:9092 --list
```

---

## 4. Logs to Inspect

### Local (Docker Compose):
```bash
# Tail Kafka logs
docker compose logs --tail=300 -f vibecheck-kafka

# Tail Zookeeper logs
docker compose logs --tail=200 -f vibecheck-zookeeper

# Search for broker errors or leader election problems
docker compose logs vibecheck-kafka | grep -iE "Fatal|LeaderNotAvailable|NotLeaderOrFollower|CorruptRecordException"
```

### Kubernetes:
```bash
kubectl logs kafka-0 -n vibecheck --tail=300
```

---

## 5. Metrics to Inspect
- `kafka_broker_up` (1 = UP, 0 = DOWN)
- `kafka_consumergroup_lag{group="notification-group"}`
- `kafka_consumergroup_lag{group="booking-group"}`
- `kafka_server_brokermessagein_total`
- Outbox pending records: `vibecheck_outbox_pending_count`

---

## 6. Safe Recovery Procedure

### Scenario A: Zookeeper Unhealthy / Desynchronized
1. If Zookeeper crashed, restart Zookeeper first and wait for healthy status:
   ```bash
   docker compose restart zookeeper
   # Wait until 'imok' responds
   until docker compose exec zookeeper sh -c 'echo "ruok" | nc localhost 2181' | grep -q 'imok'; do sleep 2; done
   ```
2. Restart Kafka broker:
   ```bash
   docker compose restart kafka
   ```

### Scenario B: Kafka Broker Crash / Disk Full
1. If storage is full, clean old retained logs (topics default to 7-day retention).
2. Restart Kafka broker. Kafka will automatically re-open log segments from disk, recover partition indexes, and announce itself to Zookeeper.
3. Microservices will automatically reconnect via Spring Kafka exponential backoff.
4. The Outbox Relay scheduler in `booking-service` will resume publishing accumulated pending events.

### Scenario C: Corrupted Topic Partition Log
If a single partition log is corrupted:
1. Identify topic and partition from Kafka log (e.g. `booking-events-0`).
2. Stop Kafka container: `docker compose stop kafka`
3. Delete only the corrupted index file (`.index` / `.timeindex`), NOT the `.log` data file. Kafka reconstructs indexes on boot.
4. Start Kafka container: `docker compose start kafka`

---

## 7. Verification
1. Verify broker metadata:
   ```bash
   docker compose exec kafka kafka-broker-api-versions --bootstrap-server localhost:9092
   ```
2. Verify test message produce and consume:
   ```bash
   # Produce test message
   echo '{"test":"dr_check"}' | docker compose exec -T kafka kafka-console-producer --bootstrap-server localhost:9092 --topic vibecheck.healthcheck.test
   # Consume test message
   docker compose exec kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic vibecheck.healthcheck.test --from-beginning --max-messages 1 --timeout-ms 5000
   ```
3. Check Outbox drain in `booking-service`:
   ```bash
   docker compose exec mysql mysql -uroot -p"$DB_ROOT_PASSWORD" -e \
     "SELECT status, count(*) FROM vibecheck_booking.outbox_events GROUP BY status;"
   ```

---

## 8. Rollback Procedure
If Kafka configuration updates disrupted listeners:
- Revert listeners in `docker-compose.yml` or `kafka-statefulset.yaml`.
- Ensure `PLAINTEXT://kafka:29092` (internal) and `PLAINTEXT_HOST://localhost:9092` (host) match existing listener configurations.

---

## 9. Escalation Conditions
- Kafka fails to start after log recovery attempts.
- Partition logs fail checksum verification across multiple topics.
- Consumer group offsets are completely lost or corrupted, requiring manual consumer offset reset.
