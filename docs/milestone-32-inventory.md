# Milestone 32 — Infrastructure Inventory

**Date:** 2026-09-28
**Platform:** VibeCheck Movie Booking Platform (Java 21 / Spring Boot microservices)
**Audit Level:** Principal SRE + Platform Engineer

---

## 1. Current Infrastructure Topology

### 1.1 Deployment Modes

| Mode | Orchestrator | File |
|---|---|---|
| Local development | Docker Compose | `docker-compose.yml` |
| CI pipeline | GitHub Actions + Maven | `.github/workflows/ci.yml` |
| Minikube / local K8s | Helm | `k8s/helm/vibecheck/values-minikube.yaml` |
| Production / staging | Helm | `k8s/helm/vibecheck/values-prod.yaml` |

### 1.2 Application Services

| Service | Port | Replicas (default) | Replicas (prod) | HPA | PDB |
|---|---|---|---|---|---|
| gateway-service | 8079 | 2 | 3-10 | YES | YES (minAvail=2) |
| auth-service | 8080 | 2 | 3-8 | YES | YES (minAvail=2) |
| movie-service | 8081 | 2 | 2-6 | YES | NO |
| theatre-service | 8082 | 2 | 2-6 | YES | NO |
| show-service | 8083 | 2 | 3-8 | YES | NO |
| booking-service | 8084 | 2 | 4-12 | YES | YES (minAvail=2) |
| payment-service | 8085 | 2 | 3-8 | YES | YES (minAvail=2) |
| notification-service | 8086 | 2 | 2-6 | YES | NO |

### 1.3 Infrastructure Components

| Component | Image | Version | Replicas | Persistence | HA |
|---|---|---|---|---|---|
| MySQL | mysql | 8.0 | 1 (StatefulSet) | 5Gi PVC | NO |
| Redis | redis | 7-alpine | 1 (StatefulSet) | 2Gi PVC AOF | NO |
| Kafka | confluentinc/cp-kafka | 7.5.0 | 1 (StatefulSet) | 5Gi PVC | CRITICAL GAP |
| ZooKeeper | confluentinc/cp-zookeeper | 7.5.0 | 1 (Deployment) | 2Gi PVC | CRITICAL GAP |

---

## 2. Current Kafka Architecture

### 2.1 Current State (Single-Broker)

```
ZooKeeper (single, Deployment, no persistent identity)
    down-arrow
Kafka Broker 1 (BROKER_ID=1, port 9092)
    down-arrow
Topics (auto-created, replication.factor=1)
```

**Key Configuration from kafka-statefulset.yaml:**
- KAFKA_BROKER_ID: "1" -- hardcoded
- KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: "1" -- SINGLE REPLICA, NO DURABILITY
- KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
- KAFKA_ADVERTISED_LISTENERS: "PLAINTEXT://kafka:9092" -- single DNS name
- KAFKA_ZOOKEEPER_CONNECT: "zookeeper:2181"

**Kafka version:** Confluent Platform 7.5.0 (Kafka 3.5.x), ZooKeeper mode confirmed.

### 2.2 Application Producer Configuration
- Docker Compose: SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:29092
- Kubernetes ConfigMap: SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
- No acks=all configuration confirmed
- No min.insync.replicas configured

### 2.3 Expected Topics
- booking-events, payment-events, notification-events
- Dead-Letter Topics (DLT variants)

---

## 3. Current MySQL Architecture

### 3.1 Current State

```
MySQL 8.0 (Single StatefulSet, 1 replica)
    down-arrow
Individual databases per service schema:
  vibecheck_auth, vibecheck_movie, vibecheck_theatre,
  vibecheck_show, vibecheck_booking, vibecheck_payment,
  vibecheck_notification
```

- Flyway: Present in booking, payment, auth, and other services
- Backup: Scripts present (Milestone 31 deliverables)
- Replication: NONE
- Production Strategy: NO managed DB strategy defined
- useSSL=false in all connection strings -- PRODUCTION SECURITY GAP

---

## 4. Current Redis Architecture

### 4.1 Current State

```
Redis 7 Alpine (Single StatefulSet, 1 replica)
    down-arrow
AOF persistence (--appendonly yes)
    down-arrow
2Gi PVC
```

- No RDB snapshot configured alongside AOF
- No Sentinel or Cluster mode
- No password authentication in Kubernetes deployment
- Used for: distributed seat locks (SET NX PX TTL), payment idempotency keys, gateway rate limiting

---

## 5. Current Secret Flow

```
values.yaml (PLAINTEXT secrets in Git)
    down-arrow
Helm template renders Kubernetes Secret vibecheck-secrets
    down-arrow
Pods consume via envFrom secretRef + env secretKeyRef
    down-arrow
Spring Boot reads environment variables
```

**Critical Issue: Secrets Committed to Git**
- dbRootPassword: "ChangeMeInProd@123" -- in values.yaml
- jwtSecret: 64-char hex value -- in values.yaml AND values-minikube.yaml
- internalSecuritySecret -- committed
- Test payment keys (Razorpay, Stripe) -- committed
- Mail credentials -- committed

**No External Secrets Operator deployed. No Vault. No AWS Secrets Manager.**

---

## 6. Existing Backup and Recovery (from Milestone 31)

### 6.1 Scripts
- scripts/backup/mysql-backup.ps1 -- mysqldump via Docker exec, gzip output
- scripts/backup/mysql-restore.ps1 -- restore via Docker exec
- scripts/backup/verify-backup.ps1 -- integrity verification
- scripts/disaster-recovery-test.ps1 -- full DR drill with isolation mode

### 6.2 Documentation
- docs/disaster-recovery-policy.md -- RPO/RTO targets
- docs/redis-recovery.md -- Redis recovery procedure
- docs/kafka-recovery.md -- Kafka recovery procedure
- docs/kubernetes-recovery.md -- K8s recovery procedure
- docs/runbooks/ -- operational runbooks

### 6.3 Point-in-Time Recovery
- NOT configured -- binary logging not enabled in MySQL StatefulSet
- NOT applicable -- no managed DB service

---

## 7. Current Load Testing

### 7.1 k6 Scripts
- tests/k6/seat-concurrency-stress-test.js
  - Scenario 1 (browsing_traffic): ramping VUs 10 to 100 over 2 minutes
  - Scenario 2 (hot_seat_race): 100 VUs, 500 shared iterations, 2 min max
  - Thresholds: p95 < 600ms, unexpected_errors == 0
  - ENV vars: GATEWAY_URL, SHOW_ID, SEAT_ID

### 7.2 Execution Status
- NEVER EXECUTED against a live multi-replica staging cluster
- No staging cluster exists
- No recorded load test results exist

---

## 8. Current CI/CD

| Workflow | Triggers | Content |
|---|---|---|
| ci.yml | push/PR to main/develop | Maven build, unit tests, OWASP scan, Docker build |
| pr-check.yml | PR events | PR validation checks |
| security-audit.yml | Scheduled/push | Security auditing |

**Missing from CI:**
- Helm lint/template validation in CI
- k6 script syntax validation
- Kubernetes manifest schema validation
- Non-root container enforcement check

---

## 9. Milestone 31 Remaining Gaps (CONDITIONAL Verdict)

| Gap | Severity | M32 Phase |
|---|---|---|
| Single-broker Kafka, replication.factor=1 | CRITICAL | Phase 2 |
| No managed HA database strategy | CRITICAL | Phase 4 |
| Secrets committed to Git in values.yaml | CRITICAL | Phase 6 |
| k6 load tests never executed on live cluster | HIGH | Phase 10 |
| ZooKeeper single instance (Deployment, no identity) | HIGH | Phase 2 |
| No PITR configured | HIGH | Phase 4/5 |
| No External Secrets Operator | HIGH | Phase 6 |
| No Kafka broker failure tests | HIGH | Phase 3 |
| Redis single instance, no Sentinel | MEDIUM | Phase 8 |
| useSSL=false in DB connection strings | MEDIUM | Phase 4 |
| No cloud provider DB commitment | MEDIUM | Phase 4 |

---

## 10. Files to Be Modified in Milestone 32

| File | Change |
|---|---|
| k8s/helm/vibecheck/templates/infrastructure/kafka-statefulset.yaml | 3-broker HA StatefulSet |
| k8s/helm/vibecheck/templates/infrastructure/kafka-service.yaml | Multi-broker headless service |
| k8s/helm/vibecheck/templates/infrastructure/zookeeper-deployment.yaml | Convert to StatefulSet |
| k8s/helm/vibecheck/templates/infrastructure/zookeeper-service.yaml | Headless ZooKeeper service |
| k8s/helm/vibecheck/templates/configmaps/common-config.yaml | Multi-broker bootstrap servers |
| k8s/helm/vibecheck/values.yaml | Kafka HA replicas, broker config |
| k8s/helm/vibecheck/values-prod.yaml | Production Kafka HA values |
| .github/workflows/ci.yml | Add helm lint + manifest validation |

---

## 11. Files NOT to Be Modified

| File | Reason |
|---|---|
| docker-compose.yml | Local dev must remain stable |
| booking-system/**/src/main/java/** | Business code preserved |
| booking-system/**/application.yml | Existing service config preserved |
| booking-system/**/db/migration/** | Flyway migrations immutable |
| scripts/disaster-recovery-test.ps1 | Existing M31 DR test preserved |
| scripts/backup/*.ps1 | Existing M31 backup scripts preserved |
| docs/disaster-recovery-policy.md | Existing M31 docs preserved |

---

## 12. Tests Executable Locally

- Helm lint/template (static)
- Maven unit tests (offline, no live infra needed)
- Docker Compose MySQL backup/restore
- Redis failure simulation (docker stop/start)
- Static secret scanning (grep values.yaml)
- Kubernetes manifest schema validation (kubeconform)

## 13. Tests Requiring Staging or Cloud Infrastructure

- 3-broker Kafka failure testing (requires multi-node K8s cluster)
- Managed DB failover (requires AWS RDS or Cloud SQL)
- Secret rotation (requires Vault or AWS Secrets Manager)
- k6 staging load test (requires running multi-replica cluster)
- HPA scaling observation (requires metrics-server + cluster)
- PDB enforcement testing (requires multi-node cluster)
- Full multi-AZ DR drill (requires cloud infrastructure)
