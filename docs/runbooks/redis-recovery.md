# Operational Runbook: Redis Recovery & Troubleshooting

**Runbook ID:** RB-OPS-003  
**Target:** Redis 7.0 (`vibecheck-redis` / `redis-0`)  
**Severity:** P1 (Seat Locking / Gateway Rate Limiting Impaired)

---

## 1. Symptoms
- Prometheus alert: `RedisDown` or `SeatLockAcquisitionFailureRate` firing.
- Gateway returns HTTP 500 when rate-limiting filter attempts Redis key inspection.
- Booking service throws `RedisConnectionException` or `Unable to acquire distributed lock for showSeatId=...`.
- Actuator health check on services reports Redis component DOWN (unless explicitly excluded via management health flags).

---

## 2. Initial Checks
1. Check whether Redis container / pod is running.
2. Check memory usage against Redis maxmemory limit.
3. Check AOF file corruption if Redis fails to boot (`Bad file format reading the append only file`).
4. Remember: Redis in VibeCheck holds ephemeral data (distributed seat locks with 10-minute TTL, rate limit windows, and payment idempotency cached keys). The authoritative ground truth for bookings and payments is **MySQL**.

---

## 3. Commands

### Local (Docker Compose):
```bash
# Check Redis container status
docker compose ps redis

# Ping Redis server directly
docker compose exec redis redis-cli ping

# Inspect Redis memory and client statistics
docker compose exec redis redis-cli info memory
docker compose exec redis redis-cli info clients
```

### Kubernetes:
```bash
# Check StatefulSet and pod
kubectl get statefulset redis -n vibecheck
kubectl get pod redis-0 -n vibecheck

# Ping Redis inside pod
kubectl exec -it redis-0 -n vibecheck -- redis-cli ping
```

---

## 4. Logs to Inspect

### Local (Docker Compose):
```bash
# Tail Redis logs
docker compose logs --tail=200 -f vibecheck-redis

# Check for persistence or memory errors
docker compose logs vibecheck-redis | grep -iE "OOM|error|appendonly|Corrupt|saving"
```

### Kubernetes:
```bash
kubectl logs redis-0 -n vibecheck --tail=200
```

---

## 5. Metrics to Inspect
- `redis_up` (1 = UP, 0 = DOWN)
- `redis_connected_clients`
- `redis_memory_used_bytes` / `redis_memory_max_bytes`
- `redis_blocked_clients`
- `rate_limit_rejected_requests_total`

---

## 6. Safe Recovery Procedure

### Scenario A: Clean Redis Restart
If Redis hung or container crashed:
1. Restart container/pod:
   ```bash
   # Docker Compose
   docker compose restart redis

   # Kubernetes
   kubectl rollout restart statefulset/redis -n vibecheck
   ```
2. Verify Redis comes back up and responds `PONG` to `redis-cli ping`.

### Scenario B: Corrupted AOF File
If Redis crashes on startup with `Bad file format reading the append only file`:
1. Run `redis-check-aof --fix`:
   ```bash
   # Docker Compose
   docker compose exec redis redis-check-aof --fix /data/appendonly.aof
   ```
2. If AOF remains unrecoverable, remove the corrupted AOF file:
   ```bash
   docker compose exec redis rm -f /data/appendonly.aof*
   docker compose restart redis
   ```
   *Impact analysis:* Ephemeral seat locks and rate limit buckets are reset. Active users may re-lock seats in booking flow; MySQL database constraints prevent double-booking at commit time.

### Scenario C: Redis Memory Exhaustion (OOM)
If Redis logs `OOM command not allowed when used memory > 'maxmemory'`:
1. Flush expired keys or flush non-persistent test cache:
   ```bash
   # Check key distribution
   docker compose exec redis redis-cli --scan --pattern "lock:*" | wc -l
   ```
2. Review evictions policy in `redis.conf` (ensure `volatile-lru` or `allkeys-lru` is active).

---

## 7. Verification
1. Verify Redis responds:
   ```bash
   docker compose exec redis redis-cli ping
   # Output: PONG
   ```
2. Test distributed lock acquisition via booking service actuator / logs.
3. Test a mock seat lock creation:
   ```bash
   docker compose exec redis redis-cli SET "lock:seat:test" "holder-uuid" EX 60 NX
   docker compose exec redis redis-cli GET "lock:seat:test"
   docker compose exec redis redis-cli DEL "lock:seat:test"
   ```

---

## 8. Rollback Procedure
If configuration changes to Redis caused failure:
- Revert command arguments in `docker-compose.yml` or `k8s/helm/vibecheck/templates/infrastructure/redis-statefulset.yaml` to standard defaults.

---

## 9. Escalation Conditions
- Redis container fails to start repeatedly even with empty storage volume.
- Redisson / Spring Data Redis client in booking-service fails to re-establish connection pool after Redis restart.
