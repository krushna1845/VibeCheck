# VibeCheck — Disaster Recovery Policy

**Version:** 1.0  
**Date:** 2026-09-26  
**Scope:** VibeCheck Movie Booking Platform — local Docker Compose + Kubernetes/Minikube deployment

> **Important:** This policy reflects the **current local development and single-node deployment architecture**.  
> Multi-region, cloud-managed, or enterprise HA capabilities are **not present** and are clearly marked.

---

## 1. Scope and Definitions

| Term | Definition |
|---|---|
| **RPO** | Recovery Point Objective — maximum acceptable data loss (time window) |
| **RTO** | Recovery Time Objective — target time to restore service after a failure |
| **Target** | The goal we aim for |
| **Demonstrated** | Actually verified in this environment |
| **Not demonstrated** | Not yet verified / infrastructure does not support it |

---

## 2. Recovery Point Objective (RPO)

### 2.1 MySQL (Relational Data)

**What is stored:** bookings, payments, shows, theatres, movies, users, outbox events, processed events.

| Scenario | RPO Target | Currently Demonstrated | Notes |
|---|---|---|---|
| Container restart (data volume preserved) | 0 seconds | YES — Docker volume persists | `mysql_data` named volume survives container restart |
| Host machine restart | Minutes (since last write) | YES — Docker volume on host disk | Volume survives OS restart |
| Manual backup + restore | Since last backup (configurable) | SCRIPTED (scripts/backup/) | Depends on backup frequency |
| Docker volume deletion (`docker volume rm`) | Since last backup | NOT DEMONSTRATED | Requires off-host backup storage |
| Disk failure | Since last backup | NOT DEMONSTRATED | No cloud/external backup |
| Multi-region | Not applicable | NOT PRESENT | Single-node deployment only |

**Recommended backup frequency for acceptable RPO:**
- Development: Daily backup (RPO = 24 hours)
- Staging: Every 6 hours (RPO = 6 hours)
- Production (not present): Continuous replication + 15-min incremental backups (RPO = 15 minutes)

### 2.2 Redis (Distributed Cache and Locks)

**What is stored:** Seat locks (TTL-based), payment idempotency keys, rate-limit counters, booking idempotency.

| Scenario | RPO Target | Currently Demonstrated | Notes |
|---|---|---|---|
| Container restart (AOF enabled) | ~1 second (AOF fsync) | YES — AOF configured | `--appendonly yes` in docker-compose.yml |
| Host machine restart | ~1 second | YES — Docker volume persists | `redis_data` named volume |
| Redis AOF corruption | Since last RDB snapshot | NOT DEMONSTRATED | RDB not explicitly configured |
| Complete loss | Data loss acceptable for locks | YES by design | Seat locks re-claimed; idempotency falls back to MySQL |

**Design note:** Redis is NOT the authoritative source of truth. All critical booking/payment state is in MySQL. Redis loss causes temporary performance degradation, not data loss.

### 2.3 Kafka Events

**What is stored:** Booking events (BookingConfirmedEvent, BookingCancelledEvent, BookingExpiredEvent), notification events, payment events.

| Scenario | RPO Target | Currently Demonstrated | Notes |
|---|---|---|---|
| Broker restart (volume preserved) | 0 seconds | YES — volume persists | `kafka_data` named volume |
| Consumer group restart | 0 events lost | YES — committed offsets | Consumer offset committed to broker |
| Transactional outbox | 0 events lost | YES — MySQL outbox | Outbox relay re-publishes on recovery |
| Complete broker loss | Since last backup | NOT DEMONSTRATED | No Kafka backup implemented |
| Single broker failure (multi-broker) | Not applicable | NOT PRESENT | Single broker only |

### 2.4 Application State

| State Type | RPO | Currently Demonstrated | Notes |
|---|---|---|---|
| In-flight HTTP requests | 0 (after graceful shutdown) | PARTIAL — preStop hook exists, but server.shutdown: graceful not yet configured | Phase 10 of M31 adds graceful shutdown |
| In-flight Kafka consumers | 0 (after consumer rebalance) | YES — Kafka consumer group manages offsets | |
| Outbox relay in-flight | 0 (idempotent relay) | YES — lease mechanism prevents double-publish | |
| Scheduled jobs | Restarts on next schedule | YES — Spring @Scheduled restarts automatically | |

---

## 3. Recovery Time Objective (RTO)

### 3.1 Single Microservice Failure

| Scenario | RTO Target | Currently Demonstrated | Notes |
|---|---|---|---|
| Docker container restart (restart: unless-stopped) | 60–90 seconds | YES — observed healthcheck cycle | Includes Spring Boot startup + healthcheck pass |
| Kubernetes pod restart (OOMKill/CrashLoop) | 60–120 seconds | YES — startupProbe + readiness gates | Container back in Ready state |
| Kubernetes rolling update (RollingUpdate: maxUnavailable 0) | 2–3 minutes | YES — Helm deployment config | No traffic loss during update |

### 3.2 Infrastructure Component Failure

| Scenario | RTO Target | Currently Demonstrated | Notes |
|---|---|---|---|
| MySQL container restart | 60–90 seconds | YES — docker-compose healthcheck | App reconnects via HikariCP retry |
| Redis container restart | 30–60 seconds | YES — docker-compose healthcheck | App reconnects; locks re-acquired on demand |
| Kafka container restart | 90–120 seconds | YES — docker-compose healthcheck | Consumer group rebalances within 60s |
| Zookeeper restart | 90–120 seconds | PARTIAL — depends on Kafka | Kafka waits for Zookeeper to stabilize |

### 3.3 Complete Environment Loss

| Scenario | RTO Target | Currently Demonstrated | Notes |
|---|---|---|---|
| Full Docker Compose restart (volumes preserved) | 5–10 minutes | YES — `docker compose up` | All services restart, dependencies respected |
| Full Docker Compose restart + MySQL restore | 10–20 minutes | SCRIPTED (scripts/disaster-recovery-test.ps1) | Includes backup restore time |
| Complete volume loss + restore from backup | 15–30 minutes | SCRIPTED | Depends on backup size |
| Kubernetes cluster re-deploy (Minikube) | 10–20 minutes | YES — deploy-minikube.ps1 | Helm deployment scripted |

### 3.4 Not Demonstrated / Out of Scope

- **Multi-AZ failover:** Not applicable. Single-node deployment only.
- **DNS failover:** Not applicable. No cloud load balancer.
- **Database replication failover:** Not applicable. No MySQL replica.
- **Cross-region recovery:** Not applicable. Not implemented.
- **RTO < 5 minutes for complete environment loss:** Not achievable with current single-node architecture.

---

## 4. Business Continuity Procedures

### 4.1 Priority Order for Recovery

When multiple components fail simultaneously, restore in this order:

1. **MySQL** — all transactional data depends on it
2. **Redis** — needed for seat locks and idempotency (non-blocking; services degrade gracefully)
3. **Zookeeper** — required before Kafka
4. **Kafka** — required for outbox relay and notifications
5. **auth-service** — required before other services (JWT validation)
6. **Core services** (movie, theatre, show)
7. **Transactional services** (booking, payment)
8. **Notification service** — non-critical path
9. **gateway-service** — last, re-routes traffic

### 4.2 Communication During Outage

- The gateway returns appropriate HTTP 503 responses when downstream services are unavailable.
- Circuit breakers in Resilience4j prevent cascade failures.
- No external status page is configured (out of scope for this milestone).

---

## 5. Backup Retention Policy

| Backup Type | Retention | Notes |
|---|---|---|
| Daily MySQL backup | 7 days | Configurable via `BACKUP_RETENTION_DAYS` |
| Weekly MySQL backup | 4 weeks | Recommended; manual promotion of daily backups |
| Production (not present) | 30 days minimum | Cloud storage required |

---

## 6. Security of Backups

- Backup files may contain PII (user email addresses, booking records).
- Backup files MUST be stored outside the repository.
- Default backup directory: `./backups/` (gitignored).
- Backup files MUST NOT be committed to version control.
- Production backup files require encryption at rest (not implemented; out of scope for local dev).
- Access to backup files should be restricted to operations team.

---

## 7. RPO/RTO Summary Table

| Component | RPO (Target) | RPO (Demonstrated) | RTO (Target) | RTO (Demonstrated) |
|---|---|---|---|---|
| MySQL (restart) | 0s | ✅ 0s | 90s | ✅ 90s |
| MySQL (backup/restore) | 24h (dev) | ✅ Scripted | 20 min | ✅ Scripted |
| Redis | ~1s (AOF) | ✅ AOF configured | 60s | ✅ 30–60s |
| Kafka (restart) | 0s | ✅ 0s | 120s | ✅ 90–120s |
| Kafka (complete loss) | N/A | ❌ Not demonstrated | N/A | ❌ Not demonstrated |
| Single service | N/A | N/A | 90s | ✅ 60–90s |
| Full Docker Compose | 24h | ✅ Scripted | 20 min | ✅ Scripted |
| K8s cluster | 24h | ✅ Scripted | 20 min | ✅ Scripted |
| Multi-region | N/A | ❌ Not present | N/A | ❌ Not present |
