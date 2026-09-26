# Redis Recovery — VibeCheck Platform

**Document Version:** 1.0  
**Milestone:** 31 — Production Operations & Disaster Recovery  
**Date:** 2026-09-26

---

## 1. Current Redis Configuration

### 1.1 Docker Compose (Local Development)

```yaml
# From docker-compose.yml
redis:
  image: redis:7-alpine
  container_name: vibecheck-redis
  restart: unless-stopped
  volumes:
    - redis_data:/data
  command: redis-server --appendonly yes
  healthcheck:
    test: ["CMD", "redis-cli", "ping"]
    interval: 10s
    timeout: 5s
    retries: 5
```

**Persistence Mode:** AOF (Append-Only File) — `--appendonly yes`  
**Volume:** Named Docker volume `redis_data` on host disk  
**Restart Policy:** `unless-stopped` — auto-restarts on crash

### 1.2 Kubernetes (Helm)

```yaml
# From redis-statefulset.yaml
command:
  - redis-server
  - --appendonly
  - "yes"
volumeMounts:
  - name: redis-data
    mountPath: /data
```

**Persistence Mode:** AOF enabled  
**Volume:** Kubernetes PVC (PersistentVolumeClaim) — 2Gi  
**Type:** StatefulSet (stable pod identity, persistent storage)

### 1.3 Persistence Strategy Assessment

| Mode | Configured | Notes |
|---|---|---|
| AOF (`--appendonly yes`) | ✅ YES | Every write operation logged to `/data/appendonly.aof` |
| RDB snapshots | ❌ NOT EXPLICIT | Redis default RDB behavior may still trigger; not explicitly configured |
| AOF fsync policy | Default (everysec) | Redis default: fsync every 1 second — at most 1 second of data loss on crash |

**Effective RPO on crash:** ~1 second (AOF everysec policy)  
**Effective RPO on volume loss:** Complete loss (no off-host backup)

---

## 2. What Data Redis Stores

### 2.1 Data Categories

| Data | Key Pattern | TTL | Loss Impact |
|---|---|---|---|
| Distributed seat locks | `seat-lock:{showId}:{seatId}` | 300s (5 min) | LOW — locks expire and are re-acquired |
| Payment idempotency keys | `idempotency:{orderId}` | 86400s (24h) | MEDIUM — duplicate payment risk for 24h window |
| Booking idempotency | `booking-idempotency:{...}` | Varies | MEDIUM — duplicate booking risk |
| Rate-limit counters | `rate-limit:{userId}` | 60s | NEGLIGIBLE — counters reset naturally |
| Webhook idempotency | `webhook-idempotency:{...}` | 604800s (7d) | MEDIUM — duplicate webhook processing risk |

### 2.2 Critical Design Point

> **Redis is NOT the authoritative source of truth for booking or payment data.**  
> All booking, payment, and outbox state is durably persisted in MySQL.

Redis contains **ephemeral operational state** that:
1. Either expires automatically via TTL
2. Or can be recovered from MySQL (e.g., idempotency keys can be rebuilt from `processed_events` table)
3. Or causes only temporary performance impact when lost (seat locks re-acquired on demand)

---

## 3. Failure Behavior

### 3.1 Redis Container Crash (AOF + Volume Intact)

**What happens:**
1. Redis container crashes.
2. Docker `restart: unless-stopped` restarts the container within seconds.
3. Redis reads AOF file on startup and replays all write operations.
4. Data restored to within ~1 second of crash.
5. Application reconnects via Spring Data Redis Lettuce pool (auto-retry).

**Data loss:** At most ~1 second of writes (AOF everysec default).  
**Service impact:** Temporary errors while Redis restarts (~10–30 seconds).

### 3.2 Redis Container Crash (Volume Intact, AOF Corrupt)

**What happens:**
1. Redis starts but AOF replay fails.
2. Redis may start with empty state (depending on AOF corruption severity).
3. Seat locks: automatically re-acquired at next booking attempt.
4. Idempotency keys: LOST — potential for duplicate payment processing within TTL window.
5. Rate-limit counters: LOST — rate limiting temporarily ineffective.

**Mitigation:** Enable both AOF AND RDB for dual-backup protection (not currently configured).

### 3.3 Redis Volume Loss (Complete Data Loss)

**What happens:**
1. Redis starts fresh with empty state.
2. Seat locks: LOST — all seats become unlockable. Self-healing within 5 minutes (TTL).
3. Payment idempotency: LOST within 24h window. Application falls back to `processed_events` MySQL table for Kafka idempotency. REST payment idempotency may briefly be at risk.
4. Rate-limiting: Resets to zero — brief window of higher-than-normal request acceptance.

**Recovery action:** See Section 5.

### 3.4 Redis Under-Memory Pressure

**What happens:**
- Redis uses LRU eviction (default: `noeviction` — blocks new writes when memory full).
- With `noeviction`, blocking writes could cause booking failures.
- Kubernetes resource limit: 512Mi memory.

**Recommendation:** Set `maxmemory-policy allkeys-lru` for cache data, but use separate Redis instance for durable idempotency data (not implemented in current architecture — single Redis instance).

---

## 4. Recovery Procedure

### 4.1 Redis Container Restart (Normal Recovery)

**When to use:** Redis container is stopped/crashed but volume is intact.

```powershell
# Step 1: Check Redis status
docker ps -a | Select-String "redis"

# Step 2: Restart Redis container
docker compose restart redis

# Step 3: Wait for healthcheck to pass
docker compose ps redis

# Step 4: Verify connectivity
docker exec vibecheck-redis redis-cli ping
# Expected: PONG

# Step 5: Check AOF restore (look for AOF loading in logs)
docker logs vibecheck-redis --tail 20

# Step 6: Verify application reconnects
curl -s http://localhost:8079/actuator/health
```

**Expected log on AOF restore:**
```
* DB loaded from append only file: 0.001 seconds
* Ready to accept connections
```

### 4.2 Redis Volume Loss (Complete Data Loss Recovery)

**When to use:** Redis data volume is corrupted or deleted.

```powershell
# Step 1: Stop Redis
docker compose stop redis

# Step 2: Remove corrupted volume (DESTRUCTIVE — only if confirmed corrupt)
docker volume rm booking-system_redis_data

# Step 3: Restart Redis (starts with clean state)
docker compose up -d redis

# Step 4: Wait for Redis to be ready
docker compose ps redis

# Step 5: Verify connectivity
docker exec vibecheck-redis redis-cli ping

# Step 6: Allow seat locks to expire naturally (max 300 seconds)
Write-Host "Seat locks will expire within 5 minutes. No manual intervention needed."

# Step 7: Verify application health
Start-Sleep -Seconds 10
curl -s http://localhost:8079/actuator/health

# Step 8: Monitor for duplicate booking/payment attempts
docker logs vibecheck-booking --tail 50 | Select-String "idempotency"
docker logs vibecheck-payment --tail 50 | Select-String "idempotency"
```

### 4.3 Kubernetes Redis Recovery

```bash
# Check Redis StatefulSet status
kubectl get statefulset redis -n vibecheck

# Check Redis pod logs
kubectl logs redis-0 -n vibecheck --tail 30

# Restart Redis pod (StatefulSet maintains PVC)
kubectl delete pod redis-0 -n vibecheck
# StatefulSet will recreate pod, PVC remains bound

# Verify recovery
kubectl exec -n vibecheck redis-0 -- redis-cli ping
```

---

## 5. Data Loss Implications After Complete Redis Loss

| Data Type | Loss Impact | Recovery Action | Time to Recover |
|---|---|---|---|
| Seat locks (distributed) | Seats momentarily unlockable | Self-healing (TTL expiry) | 0–300 seconds |
| Payment idempotency (REST) | Duplicate REST payment risk | Monitor payment logs; MySQL `processed_events` provides Kafka dedup | Immediate via Kafka; REST risk until next payment attempt |
| Rate-limit counters | Brief over-acceptance | Counters reset within 60s | 60 seconds |
| Webhook idempotency | Duplicate webhook processing risk | Review MySQL payment records for duplicates | Manual review recommended |
| Session data | None | No session data stored in Redis | N/A |

**Critical note:** The `processed_events` MySQL table provides Kafka-level idempotency independently of Redis. Kafka consumers will NOT re-process already-processed events even after Redis loss.

---

## 6. Verification Procedure

### 6.1 Verify Redis is Running

```powershell
# Docker Compose
docker exec vibecheck-redis redis-cli ping
# Expected: PONG

docker exec vibecheck-redis redis-cli info persistence | Select-String "aof"
# Expected: aof_enabled:1

# Kubernetes
kubectl exec -n vibecheck redis-0 -- redis-cli ping
kubectl exec -n vibecheck redis-0 -- redis-cli info persistence
```

### 6.2 Verify AOF is Active

```powershell
docker exec vibecheck-redis redis-cli config get appendonly
# Expected: appendonly yes

docker exec vibecheck-redis redis-cli info persistence
# Look for:
#   aof_enabled:1
#   aof_rewrite_in_progress:0
#   aof_last_write_status:ok
```

### 6.3 Verify Application Redis Connectivity

```powershell
# Check booking service (uses Redis for seat locks)
curl -s http://localhost:8079/actuator/health
# Expected: {"status":"UP",...}

# Check for Redis-related errors in logs
docker logs vibecheck-booking --tail 20 | Select-String -Pattern "redis|Redis|connection"
docker logs vibecheck-payment --tail 20 | Select-String -Pattern "redis|Redis|connection"
```

---

## 7. Recommendations for Improved Resilience

> These are recommendations for future improvements. They are **not implemented** in the current architecture.

| Recommendation | Benefit | Effort |
|---|---|---|
| Enable RDB snapshots alongside AOF | Dual protection; faster restart | Low |
| Configure `maxmemory` and `maxmemory-policy` | Prevent write-blocking under memory pressure | Low |
| Separate Redis instances for locks vs. idempotency | Independent failure domains | Medium |
| Redis Sentinel (Docker Compose) | Automatic failover, no data loss | High |
| Redis Cluster (Production K8s) | HA + horizontal scaling | High |
| Cloud-managed Redis (e.g., ElastiCache, Cloud Memorystore) | Managed HA, automated backups | High |
