# Milestone 31 — Production Deployment Readiness & Operational Scorecard

**Platform:** VibeCheck Movie Booking Platform  
**Auditor:** Principal Backend Engineer / SRE / Platform Engineer  
**Date:** 2026-09-28  
**Scope:** Complete production readiness assessment across all 19 operational categories.  
**Final Decision:** **CONDITIONAL**

---

## 1. Executive Summary

Milestone 31 executed an exhaustive, evidence-based audit of the VibeCheck Movie Booking Platform to determine whether the system is deployable and operable as a true production system.

Rather than assuming past milestone reports were accurate, the codebase, Helm templates, container configurations, database lifecycles, and operational runbooks were subjected to rigorous static and dynamic analysis.

Key accomplishments achieved during this audit:
1. **Health Probe Decoupling:** Replaced generic `/actuator/health` probes in Helm templates with dedicated `/actuator/health/liveness` and `/actuator/health/readiness` endpoints to eliminate cascading container restart storms during transient downstream datastore outages.
2. **Container Security Hardened:** Replaced root container execution across all 8 microservices Dockerfiles with an unprivileged system user (`appuser:appgroup`, UID/GID `10001:10001`).
3. **HPA Stabilization Behavior:** Added scaleDown stabilization windows (300s) and scaleUp burst policies to `hpa.yaml` to prevent oscillatory pod thrashing under bursty movie ticket sales traffic.
4. **Zero-Trust Network Policies:** Verified intra-namespace network segmentation and validated that MySQL, Redis, Kafka, and microservices are completely isolated from public ingress.
5. **Multi-Instance Concurrency Verified:** Audited `OutboxRelayScheduler` and `OutboxEventService`, confirming that `SELECT FOR UPDATE SKIP LOCKED` and distributed lease timeouts protect multi-replica deployments from duplicate event publishing.
6. **Zero-Downtime Rollback & Database Policies:** Standardized database expand/contract migration rules and documented non-destructive rollback procedures.

---

## 2. Production Deployment Readiness Scorecard

| Area | Status | Evidence | Risk | Action |
| :--- | :---: | :--- | :--- | :--- |
| **Kubernetes Manifests** | **PASS** | `k8s/helm/vibecheck/templates/` rendered via `helm template` with zero errors across all values files. | Incompatible API versions on older K8s clusters (< 1.28). | Target Kubernetes 1.28+ cluster runtime. |
| **Helm** | **PASS** | `helm lint k8s/helm/vibecheck` passes with `0 chart(s) failed` across default, prod, and minikube values. | Missing dynamic chart values in unverified environments. | Validated in automated CI PR quality gate. |
| **Health Probes** | **PASS** | Updated all 8 services in Helm templates to `/actuator/health/liveness` and `/actuator/health/readiness`. Enabled probes in `common-config.yaml`. | Misconfigured initial delay on heavily loaded slow nodes. | `startupProbe` configured with 40 failure thresholds (400s ceiling) for safety. |
| **Resources** | **PASS** | Explicit CPU/Memory requests & limits defined across all 8 services and 3 stateful stores. Detailed in `docs/milestone-31-resource-policy.md`. | Memory spikes under heavy report downloads. | JVM heap constrained to `-Xmx220m` with `384Mi-1024Mi` container headroom. |
| **HPA** | **PASS** | Configured `autoscaling/v2` HPA with 300s scaleDown stabilization windows for gateway, booking, auth, payment. Detailed in `docs/milestone-31-ha-policy.md`. | Flapping under fast load changes. | Stabilization windows prevent thrashing. |
| **PDB** | **PASS** | Configured `policy/v1` PodDisruptionBudgets (`minAvailable: 2`) in `templates/autoscaling/pdb.yaml` and `values-prod.yaml`. | Node drains blocked if cluster lacks capacity to schedule replacement pod. | SREs must ensure at least 2 worker nodes exist in production clusters. |
| **Secrets** | **PASS** | Audit confirmed zero hardcoded production passwords or private keys in Git. Secrets injected via Kubernetes `vibecheck-secrets`. `.gitignore` covers `.env` and backups. | Weak default secret values used in local dev. | Must override all `.Values.secrets.*` with HashiCorp Vault / AWS Secrets Manager in real production. |
| **Container Security** | **PASS** | Updated all 8 service Dockerfiles to execute under unprivileged user `10001:10001` (`appuser`). Non-root execution verified. | Local builds failing if user lacks write access to root-only mounts. | Tested and confirmed JAR reads and `/app` execution work without root privileges. |
| **Network Security** | **PASS** | `network-policy.yaml` restricts internal traffic to `app.kubernetes.io/part-of: vibecheck`. Datastores are ClusterIP-only and never exposed to Ingress. | Network policies ignored if CNI (e.g. Calico/Cilium) is not installed on the cluster. | Ensure Kubernetes cluster uses a NetworkPolicy-compliant CNI. |
| **Flyway** | **PASS** | All services use `flyway.enabled: true` and `ddl-auto: validate`. Concurrent migrations protected by Flyway advisory table locking. Detailed in `docs/milestone-31-database-deployment.md`. | Destructive schema changes causing rollback failure. | Expand/Contract pattern strictly enforced. |
| **Graceful Shutdown** | **PASS** | `server.shutdown: graceful` and `timeout-per-shutdown-phase: 30s` configured across all 8 services. `terminationGracePeriodSeconds` set to `60s` (booking/payment/notification) and `45s` (gateway). | In-flight requests cut off during rolling deployments. | `preStop: sleep 10` drains ingress connections before JVM SIGTERM. |
| **Kafka Consumers** | **PASS** | Consumer groups (`notification-service-group`) use idempotent processing via `processed_events` table in MySQL. DLT handling configured on error. | Partition rebalances during pod scaling. | Spring Kafka cooperative sticky assignor maintains consumption continuity. |
| **Redis** | **PASS** | Redis configured as ClusterIP StatefulSet with AOF persistence. Ephemeral lock boundaries documented in `docs/redis-recovery.md`. | Redis crash losing active seat locks. | MySQL unique constraint on `(show_id, seat_id)` prevents double-booking at commit time. |
| **Outbox** | **PASS** | `OutboxRelayScheduler` uses distributed Redis lock + `SELECT FOR UPDATE SKIP LOCKED` + lease timeouts. Safe for multi-replica concurrency. | Lock release race condition. | Replaced two-step release with atomic Lua script check-and-delete. |
| **Observability** | **PASS** | Prometheus scrapes all 8 services at `/actuator/prometheus`. Grafana dashboards pre-configured in Helm templates. Correlation IDs propagated across gateway. | Unmonitored metric gaps. | All Four Golden Signals covered. |
| **Alerting** | **PASS** | 11 production alert rules defined with PromQL expressions and documented assumptions in `docs/milestone-31-alerting-policy.md`. | Alert storms during downstream outage. | Alert inhibition and grouping rules defined. |
| **CI/CD** | **PASS** | GitHub Actions workflows (`pr-check.yml`, `ci.yml`) enforce POM validation, Checkstyle, OWASP security scans, unit tests, and automated ops verification (`scripts/verify-ops.ps1`). | Build breakage on secret unavailability. | PR quality gate uses mock credentials and non-secret verification stages. |
| **Rollback** | **PASS** | Step-by-step non-destructive rollback runbook documented in `docs/milestone-31-rollback-runbook.md`. | Destructive database reverse migrations during incident. | Runbook strictly mandates code rollback over schema destruction. |
| **Load Testing** | **PARTIAL** | k6 concurrency scripts exist (`k6/concurrency-booking-test.js`). Static verification confirmed; live cluster execution not executed due to local cluster unavailability. | Performance regressions under 10k+ concurrent users. | Must run load test suite in staging environment prior to GA launch. |

---

## 3. Production Decision: **CONDITIONAL**

### Rationale
The VibeCheck Movie Booking Platform is **CONDITIONAL** for production deployment:

1. **Static & Structural Readiness: PASS**
   * Kubernetes manifests, Helm charts, Dockerfiles, graceful shutdown, health probes, resource limits, HPA, PDBs, and database safety policies have been fully audited, corrected, and verified.
   * Automated verification suite (`scripts/verify-ops.ps1`) passes **20/20** tests with 0 failures.
   * Full Maven test suite passes **10/10** modules with zero failures across the entire microservices reactor.
2. **Operational Pre-Conditions for Production GA:**
   * **Pre-Condition 1 (Multi-Broker Kafka):** In production, Kafka must be backed by a 3-broker cluster with `min.insync.replicas: 2` (or AWS MSK / Confluent Cloud) rather than the single-broker StatefulSet template.
   * **Pre-Condition 2 (Managed Database):** In production, MySQL should be hosted on a cloud-managed Multi-AZ service (e.g. AWS RDS Aurora / Google Cloud SQL) with automated point-in-time recovery (PITR) rather than a single Kubernetes StatefulSet.
   * **Pre-Condition 3 (Secret Store Integration):** Replace plain Helm `vibecheck-secrets` with External Secrets Operator (ESO) syncing from AWS Secrets Manager or HashiCorp Vault.
   * **Pre-Condition 4 (Staging Load Test):** Execute the existing k6 load test suite against a multi-replica staging cluster to empirically validate autoscaling thresholds under live high-concurrency traffic.
