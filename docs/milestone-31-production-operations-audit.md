# Milestone 31 — Production Operations Audit

**Date:** 2026-09-26
**Platform:** VibeCheck Movie Booking Platform (Java 21 + Spring Boot microservices)

---

## 1. Files Inspected

| File / Path | Purpose |
|---|---|
| `docker-compose.yml` | Primary local deployment orchestration |
| `.env` | Local environment secrets |
| `init-mysql.sql` | MySQL database initialization |
| `booking-system/booking-service/src/main/resources/application.yml` | Booking service config |
| `booking-system/payment-service/src/main/resources/application.yml` | Payment service config |
| `booking-system/gateway-service/src/main/resources/application.yml` | Gateway config |
| `booking-system/notification-service/src/main/resources/application.yml` | Notification config |
| `booking-system/booking-service/src/main/resources/db/migration/V6__create_outbox_and_processed_events.sql` | Outbox schema |
| `k8s/helm/vibecheck/values.yaml` | Helm default values |
| `k8s/helm/vibecheck/templates/infrastructure/mysql-statefulset.yaml` | MySQL K8s StatefulSet |
| `k8s/helm/vibecheck/templates/infrastructure/redis-statefulset.yaml` | Redis K8s StatefulSet |
| `k8s/helm/vibecheck/templates/infrastructure/kafka-statefulset.yaml` | Kafka K8s StatefulSet |
| `k8s/helm/vibecheck/templates/services/booking-service.yaml` | Booking service K8s Deployment |
| `k8s/helm/vibecheck/templates/monitoring/prometheus-configmap.yaml` | Prometheus scrape config |
| `.github/workflows/ci.yml` | CI/CD pipeline |
| `booking-system/docs/OBSERVABILITY.md` | Observability guide |
| `k8s/README.md` | K8s deployment guide |

---

## 2. Current Capabilities

### 2.1 Infrastructure

| Component | Config | Persistence | Notes |
|---|---|---|---|
| MySQL | docker-compose.yml | Named Docker volume mysql_data | restart: unless-stopped, healthcheck present |
| Redis | docker-compose.yml | Named Docker volume redis_data, AOF enabled (--appendonly yes) | restart: unless-stopped, healthcheck present |
| Kafka | docker-compose.yml | Named Docker volume kafka_data | restart: unless-stopped, healthcheck present |
| Zookeeper | docker-compose.yml | Named Docker volumes zookeeper_data, zookeeper_logs | restart: unless-stopped, healthcheck present |

### 2.2 Application Services

- All 8 services have `restart: unless-stopped` in Docker Compose.
- All 8 services expose `/actuator/health` with Docker Compose healthchecks.
- Kubernetes: All services have startupProbe, readinessProbe, and livenessProbe.
- Kubernetes: All services have preStop: sleep 10 lifecycle hook.
- Kubernetes: MySQL, Redis, Kafka use StatefulSet with PersistentVolumeClaim.
- Kubernetes: Services use RollingUpdate with maxUnavailable: 0.

### 2.3 Resilience

- Resilience4j circuit breaker configured in gateway-service.
- Redis failure handled gracefully (non-fatal for most services).
- Transactional Outbox pattern in booking-service with Kafka relay.
- Dead-Letter Topic (DLT) routing implemented.
- Payment idempotency via Redis (24h TTL) and MySQL processed_events table.
- Kafka consumer idempotency via processed_events table.
- Seat lock TTL-based cleanup scheduler.

### 2.4 Monitoring

- Prometheus scrapes all 8 services via /actuator/prometheus.
- Grafana dashboard deployed in K8s.
- All services export Micrometer metrics.
- Distributed tracing via Brave/Zipkin/Jaeger.
- Structured JSON logging with correlation IDs.

### 2.5 CI/CD

- GitHub Actions: build, test, OWASP security scan, Docker build.
- Helm lint validated in K8s scripts.
- PR check workflow present.

---

## 3. Missing Capabilities

| Gap | Severity | Phase Addressing It |
|---|---|---|
| No MySQL backup script | CRITICAL | Phase 3 |
| No MySQL restore script | CRITICAL | Phase 4 |
| No backup integrity verification | HIGH | Phase 5 |
| No server.shutdown: graceful in any service | HIGH | Phase 10 |
| No spring.lifecycle.timeout-per-shutdown-phase set | MEDIUM | Phase 10 |
| No Kubernetes terminationGracePeriodSeconds explicit setting | MEDIUM | Phase 11 |
| No PodDisruptionBudget for critical services | MEDIUM | Phase 11 |
| No disaster-recovery runbooks | HIGH | Phase 12 |
| No alerting rules (Prometheus AlertManager) | MEDIUM | Phase 14 |
| No Redis recovery documentation | MEDIUM | Phase 6 |
| No Kafka recovery documentation | MEDIUM | Phase 7 |
| No disaster recovery test script | HIGH | Phase 8 |
| No RPO/RTO policy document | HIGH | Phase 2 |

---

## 4. Risks Identified

### 4.1 Data Loss Risk
- MySQL: No backup = complete data loss on docker volume rm or host failure.
- Redis: AOF persistence configured but no documented recovery procedure.
- Kafka: Single broker. Log retention is subject to disk pressure.

### 4.2 Secrets Risk
- .env contains development credentials. Not committed to secrets managers.
- values.yaml contains placeholder secrets. Not for production use.
- Backup scripts must NOT embed credentials.

### 4.3 Graceful Shutdown Risk
- Without server.shutdown: graceful, Spring Boot services hard-kill in-flight requests on SIGTERM.
- Particularly risky for: booking-service, payment-service, outbox relay scheduler.

### 4.4 Single Points of Failure
- Single Kafka broker (no replication in dev).
- Single Zookeeper instance.
- Single MySQL instance (no replication in dev).
- Single Redis instance (no Sentinel/Cluster in dev).

---

## 5. Proposed Changes

| Phase | Change | Priority |
|---|---|---|
| 2 | docs/disaster-recovery-policy.md - RPO/RTO | CRITICAL |
| 3 | scripts/backup/mysql-backup.ps1 | CRITICAL |
| 4 | scripts/backup/mysql-restore.ps1 | CRITICAL |
| 5 | scripts/backup/verify-backup.ps1 | HIGH |
| 6 | docs/redis-recovery.md | HIGH |
| 7 | docs/kafka-recovery.md | HIGH |
| 8 | scripts/disaster-recovery-test.ps1 | HIGH |
| 10 | Add server.shutdown: graceful to all service application.yml | HIGH |
| 11 | Add terminationGracePeriodSeconds, PodDisruptionBudget to K8s | MEDIUM |
| 12 | Create runbooks in docs/runbooks/ | HIGH |
| 13 | Document existing metrics coverage | MEDIUM |
| 14 | docs/alerting.md + Prometheus alert rules ConfigMap | MEDIUM |
| 15 | CI/CD: compose validate, helm lint, script syntax check | MEDIUM |

---

## 6. Verification Plan

Each deliverable will be verified by:
1. Existence check - file created at expected path
2. Syntax check - scripts validated for PowerShell syntax
3. Docker Compose - docker compose config re-run after changes
4. Helm - helm lint + helm template re-run after Helm changes
5. Graceful shutdown - application.yml change confirmed with grep
6. Backup/restore - script logic verified; actual execution requires running Docker environment
7. Documentation - markdown files checked for completeness
