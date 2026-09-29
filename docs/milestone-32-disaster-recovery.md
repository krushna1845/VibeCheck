# Milestone 32 — Disaster Recovery Runbook

**Date:** 2026-09-28
**Version:** 1.0
**Audience:** On-call SRE, Platform Engineer

---

## DR-01: Kafka Broker Failure

### Failure Signature
- Kafka producer errors in service logs
- Consumer lag spike in Prometheus
- `kafka-broker-api-versions` timeout on a specific broker
- Alert: `KafkaConsumerLagHigh` or `KafkaProducerErrors`

### Expected Behavior (3-broker HA)
- Remaining 2 brokers continue serving traffic
- Leader election completes within 30 seconds
- Consumer group rebalances within 60 seconds
- Outbox relay retries on next scheduler tick

### Recovery Steps
```powershell
# 1. Identify which broker failed
kubectl get pods -n vibecheck -l app.kubernetes.io/name=kafka

# 2. Check broker logs
kubectl logs kafka-<N> -n vibecheck --tail=100

# 3. For StatefulSet pods, Kubernetes auto-restarts failed pods
# Wait for pod to restart and rejoin (typically 60-120 seconds)

# 4. Verify broker rejoined
kubectl exec -n vibecheck kafka-0 -- kafka-broker-api-versions \
  --bootstrap-server kafka-0.kafka-headless.vibecheck.svc.cluster.local:9092

# 5. Verify ISR recovery
kubectl exec -n vibecheck kafka-0 -- kafka-topics \
  --bootstrap-server kafka-0.kafka-headless.vibecheck.svc.cluster.local:9092 \
  --describe

# 6. Check consumer group offsets
kubectl exec -n vibecheck kafka-0 -- kafka-consumer-groups \
  --bootstrap-server kafka-0.kafka-headless.vibecheck.svc.cluster.local:9092 \
  --describe --all-groups

# 7. Verify outbox relay recovered
kubectl logs -n vibecheck deployment/booking-service --tail=50 | grep -i outbox
```

### Data Integrity Check
```sql
-- Check for outbox records stuck in PROCESSING (should be 0 after recovery)
SELECT COUNT(*) FROM vibecheck_booking.outbox_events WHERE status = 'PROCESSING';

-- Check for dead-letter events accumulated during outage
SELECT COUNT(*) FROM vibecheck_booking.outbox_events WHERE status = 'DEAD_LETTER';
```

### RTO Target: < 5 minutes (Kubernetes auto-restart + leader election)
### RPO: 0 (no data loss with replication.factor=3, min.insync.replicas=2)

---

## DR-02: Redis Failure

### Failure Signature
- Booking service logs: "RedisConnectionException"
- All new bookings fail with seat-lock acquisition error
- Prometheus: `redis_connected_clients` drops to 0

### Expected Behavior
- Existing bookings in CONFIRMED state are unaffected (persisted to MySQL)
- New booking attempts fail with appropriate error (seat lock cannot be acquired)
- Payment idempotency fallback: `processed_events` table provides secondary guard
- Gateway rate limiting falls back to per-replica state

### Recovery Steps
```powershell
# 1. Check Redis pod status
kubectl get pod redis-0 -n vibecheck

# 2. Check Redis logs
kubectl logs redis-0 -n vibecheck --tail=100

# 3. If pod is CrashLooping, describe to find cause
kubectl describe pod redis-0 -n vibecheck

# 4. For PVC corruption, check PVC status
kubectl get pvc redis-data-redis-0 -n vibecheck

# 5. Force pod restart
kubectl delete pod redis-0 -n vibecheck

# 6. Wait for Redis to restart and verify AOF recovery
kubectl logs redis-0 -n vibecheck | grep -i "aof\|ready\|loaded"

# 7. Verify connectivity
kubectl exec -n vibecheck deployment/booking-service -- \
  sh -c "redis-cli -h redis -p 6379 ping"
```

### Lock State After Recovery
- All seat locks with TTL < restart duration have expired automatically
- Seats that were LOCKED but payment not completed are now AVAILABLE again
- No false lock ownership possible (TTL-based expiry + Lua ownership check)

### Data Integrity Check
```sql
-- Check for bookings that may have been affected by Redis outage
-- These should be in PENDING state only (CONFIRMED = successful)
SELECT status, COUNT(*) FROM vibecheck_booking.bookings
WHERE created_at > NOW() - INTERVAL 30 MINUTE
GROUP BY status;
```

### RTO Target: < 3 minutes (K8s auto-restart + AOF replay)
### RPO: Near-zero for persistent data (AOF); ephemeral locks are intentionally lost

---

## DR-03: Application Pod Failure

### Failure Signature
- Kubernetes pod in CrashLoopBackOff or OOMKilled
- Service health check failing
- Alert: `PodRestartRateHigh`

### Recovery Steps
```powershell
# 1. Identify failed pods
kubectl get pods -n vibecheck | grep -v Running

# 2. Check pod events
kubectl describe pod <pod-name> -n vibecheck

# 3. Check logs (including previous terminated container)
kubectl logs <pod-name> -n vibecheck --previous

# 4. For OOMKilled: check JVM heap settings
# JAVA_TOOL_OPTIONS in common-config ConfigMap: -Xmx220m
# If pod OOMs consistently, increase memory limits in values.yaml

# 5. For CrashLoop due to dependency: check readiness/liveness probe
# Services have initContainers waiting for MySQL and Kafka

# 6. Force restart (Deployment auto-restarts but you can accelerate)
kubectl rollout restart deployment/<service-name> -n vibecheck

# 7. Monitor rollout
kubectl rollout status deployment/<service-name> -n vibecheck
```

### RTO Target: < 2 minutes (Kubernetes self-healing)
### RPO: 0 (stateless application, state in MySQL/Redis/Kafka)

---

## DR-04: Booking Service Replica Termination

### Failure Signature
- Booking service pod terminated (maintenance, OOM, or crash)
- In-flight booking requests may be dropped

### Graceful Shutdown Behavior
- terminationGracePeriodSeconds: 60
- preStop sleep: 10 seconds (drains load balancer)
- Spring Boot graceful shutdown (server.shutdown: graceful)
- In-flight HTTP requests complete before process exits

### Recovery Steps
- Kubernetes automatically starts replacement pod
- New pod passes startupProbe (httpGet /actuator/health/liveness)
- New pod receives traffic only after readinessProbe passes
- No manual intervention required for single replica failure

### Outbox During Failure
- If pod dies with outbox PROCESSING records:
  - Records remain in PROCESSING state in MySQL
  - On next relay tick (whichever surviving replica picks it up): records are re-published
  - Consumer idempotency (processed_events) prevents duplicate processing

### Data Integrity Check
```sql
SELECT status, COUNT(*) FROM vibecheck_booking.outbox_events
WHERE updated_at > NOW() - INTERVAL 5 MINUTE GROUP BY status;
```

---

## DR-05: Payment Service Replica Termination

### Failure Signature
- Payment service pod terminated
- In-flight payment requests may time out

### Graceful Shutdown Behavior
- Same as booking service: 60s grace period, preStop 10s drain
- In-flight payment processing requests complete
- Webhook receiver: in-flight webhook validation completes before shutdown

### Idempotency Protection
- Payment idempotency via Redis TTL key (24h) + `processed_events` table
- Even if payment webhook is replayed after pod restart, duplicate payment is prevented
- Razorpay/Stripe webhooks have built-in retry -- new pod handles replayed webhooks

### Recovery Steps
```powershell
# Monitor payment service restart
kubectl get pod -n vibecheck -l app.kubernetes.io/name=payment-service -w

# Check payment processing after restart
kubectl logs -n vibecheck deployment/payment-service --tail=50 | grep -i "payment\|webhook\|processed"
```

### Data Integrity Check
```sql
-- Verify no duplicate payments exist
SELECT idempotency_key, COUNT(*) c FROM vibecheck_payment.payments
GROUP BY idempotency_key HAVING c > 1;

-- Verify no orphaned payment attempts
SELECT COUNT(*) FROM vibecheck_payment.payments WHERE status = 'PENDING'
AND created_at < NOW() - INTERVAL 30 MINUTE;
```

---

## DR-06: Database Failover / Recovery

### For Managed Database (Production Target)
- Automated failover: managed by cloud provider (AWS RDS, Cloud SQL)
- Failover duration: 30-120 seconds depending on provider
- Connection string: same endpoint (CNAME / load balancer)
- Application: Spring Boot connection pool retries automatically

### For Kubernetes StatefulSet (Staging / Local)
```powershell
# 1. Check MySQL pod status
kubectl get pod mysql-0 -n vibecheck

# 2. If crashed, describe
kubectl describe pod mysql-0 -n vibecheck

# 3. Force restart
kubectl delete pod mysql-0 -n vibecheck
# Kubernetes recreates the pod with same PVC

# 4. Wait for MySQL ready
kubectl wait --for=condition=Ready pod/mysql-0 -n vibecheck --timeout=120s

# 5. Verify all applications reconnect (Spring Boot auto-reconnects)
kubectl rollout restart deployment/booking-service -n vibecheck
kubectl rollout restart deployment/payment-service -n vibecheck
```

### Restore from Backup (Complete Data Loss)
```powershell
# 1. Start fresh MySQL
# 2. Restore from latest backup
.\scripts\backup\mysql-restore.ps1 -BackupFile .\backups\mysql\<latest>.sql.gz

# 3. Verify restoration
.\scripts\milestone-32-database-recovery-test.ps1 -Mode existing

# 4. Restart all services (Flyway validates schema on startup)
docker compose restart  # or kubectl rollout restart
```

### RTO Target: < 60 seconds (managed DB); < 5 minutes (StatefulSet restart + Flyway)
### RPO Target: < 5 minutes (continuous binary log shipping to managed backup)

---

## DR-07: Kafka Consumer Restart

### Scenario
- Consumer group (booking-service, notification-service) restarts
- Group rebalance required

### Recovery Steps
```powershell
# 1. Monitor consumer group rebalance
kubectl exec -n vibecheck kafka-0 -- kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --group booking-service-consumer-group

# 2. Watch consumer lag
kubectl exec -n vibecheck kafka-0 -- kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe --all-groups | grep -E "LAG|consumer"

# 3. Consumer group recovers automatically
# Spring Kafka reconnects and commits offsets

# 4. Verify no duplicate processing
```

### Expected Behavior
- Consumer group rebalance: typically 30-60 seconds
- Uncommitted offsets are replayed from last committed position
- Duplicate messages handled by `processed_events` table (idempotency guard)
- No duplicate booking, payment, or notification events reach the application

---

## Drill Execution Checklist

Before running any DR drill:
- [ ] Confirm staging/isolated environment (not production)
- [ ] Confirm backup exists and is verified
- [ ] Notify team of planned drill
- [ ] Document starting state (bookings, payments, outbox)
- [ ] Have rollback plan ready

After each drill:
- [ ] Verify all services healthy
- [ ] Verify data integrity (run integrity SQL checks)
- [ ] Record RTO (time to recover)
- [ ] Record any data loss (RPO verification)
- [ ] Update runbook with any new learnings
