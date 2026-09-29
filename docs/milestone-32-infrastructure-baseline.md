# Milestone 32 — Infrastructure Baseline Audit

**Date:** 2026-09-28
**Auditor:** Principal SRE + Platform Engineer
**Method:** Static inspection of all Kubernetes/Helm artifacts + helm lint/template execution

---

## 1. Helm Chart Baseline

### 1.1 Helm Lint Result

```
==> Linting k8s/helm/vibecheck
[INFO] Chart.yaml: icon is recommended

1 chart(s) linted, 0 chart(s) failed
```

**Status: PASS** (INFO only: icon is recommended, not required)

### 1.2 Helm Template Result

**Status: PASS** -- helm template renders without errors for default values.

### 1.3 Helm Chart Structure

```
k8s/helm/vibecheck/
  Chart.yaml               (version: 1.0.0, apiVersion: v2)
  values.yaml              (default values)
  values-prod.yaml         (production overrides)
  values-minikube.yaml     (minikube overrides)
  templates/
    _helpers.tpl           (common helpers: labels, waitForMySQL, waitForKafka)
    namespace.yaml
    autoscaling/
      hpa.yaml             (HPA for services with autoscaling.enabled=true)
      pdb.yaml             (PDB for services with pdb.enabled=true)
    configmaps/
      common-config.yaml   (shared env vars for all services)
      mysql-init-config.yaml
    infrastructure/
      kafka-statefulset.yaml
      kafka-service.yaml
      mysql-statefulset.yaml
      mysql-service.yaml
      redis-statefulset.yaml
      redis-service.yaml
      zookeeper-deployment.yaml (NOT StatefulSet -- identity risk)
      zookeeper-service.yaml
    ingress/
    monitoring/
      grafana-configmap.yaml
      grafana-dashboard-configmap.yaml
      grafana-deployment.yaml
      prometheus-configmap.yaml
      prometheus-deployment.yaml
    secrets/
      vibecheck-secrets.yaml
    security/
      network-policy.yaml
    services/
      auth-service.yaml
      booking-service.yaml
      gateway-deployment.yaml
      gateway-service.yaml
      movie-service.yaml
      notification-service.yaml
      payment-service.yaml
      show-service.yaml
      theatre-service.yaml
```

---

## 2. StatefulSets Audit

### 2.1 MySQL StatefulSet

| Property | Value | Status |
|---|---|---|
| Replicas | 1 | SINGLE POINT OF FAILURE |
| Persistence | 5Gi PVC, ReadWriteOnce | OK |
| Health Probe | mysqladmin ping liveness + readiness | OK |
| StartupProbe | NOT configured | MISSING |
| Init SQL | mounted from ConfigMap | OK |
| Security Context | NOT configured | MISSING |
| Non-root | NOT configured | MISSING |
| Resource Limits | 1 CPU / 1Gi RAM | OK |
| Resource Requests | 250m CPU / 512Mi RAM | OK |

### 2.2 Redis StatefulSet

| Property | Value | Status |
|---|---|---|
| Replicas | 1 | SINGLE POINT OF FAILURE |
| Persistence | 2Gi PVC, ReadWriteOnce + AOF | OK |
| StartupProbe | tcpSocket :6379, failureThreshold=24 | OK |
| ReadinessProbe | tcpSocket :6379 | OK |
| LivenessProbe | tcpSocket :6379 | OK |
| Authentication | NONE | SECURITY GAP |
| Resource Limits | 500m CPU / 512Mi RAM | OK |
| Resource Requests | 100m CPU / 128Mi RAM | OK |
| Non-root | NOT configured | MISSING |

### 2.3 Kafka StatefulSet

| Property | Value | Status |
|---|---|---|
| Replicas | 1 | CRITICAL - NO HA |
| BROKER_ID | "1" hardcoded | CRITICAL - HA incompatible |
| Replication Factor | 1 (offsets topic) | CRITICAL - NO DURABILITY |
| min.insync.replicas | NOT SET | CRITICAL - MISSING |
| Persistence | 5Gi PVC | OK |
| StartupProbe | tcpSocket :9092, failureThreshold=30 | OK |
| ReadinessProbe | tcpSocket :9092 | OK |
| LivenessProbe | tcpSocket :9092 | OK |
| Advertised Listeners | PLAINTEXT://kafka:9092 only | INSUFFICIENT for HA |
| Resource Limits | 1 CPU / 1Gi RAM | OK |

### 2.4 ZooKeeper Deployment

| Property | Value | Status |
|---|---|---|
| Kind | Deployment (NOT StatefulSet) | CRITICAL - identity risk |
| Replicas | 1 | SINGLE POINT OF FAILURE |
| Persistence | NOT configured (stateless Deployment) | DATA LOSS RISK |
| StartupProbe | tcpSocket :2181, failureThreshold=60 | OK |
| ReadinessProbe | tcpSocket :2181 | OK |
| LivenessProbe | tcpSocket :2181 | OK |

**Critical Finding:** ZooKeeper is deployed as a Deployment, not a StatefulSet. This means:
1. ZooKeeper has no stable network identity
2. ZooKeeper has no persistent storage -- data is ephemeral
3. When ZooKeeper restarts, Kafka brokers must re-register

---

## 3. Application Service Deployments Audit

### 3.1 Common Properties (booking-service as representative)

| Property | Value | Status |
|---|---|---|
| RollingUpdate | maxSurge=25%, maxUnavailable=0 | OK |
| terminationGracePeriodSeconds | 60 | OK |
| preStop sleep | 10s | OK |
| StartupProbe | httpGet /actuator/health/liveness | OK |
| ReadinessProbe | httpGet /actuator/health/readiness | OK |
| LivenessProbe | httpGet /actuator/health/liveness | OK |
| InitContainers | waitForMySQL + waitForKafka | OK |
| PodAntiAffinity | preferred, hostname topology | OK (when enabled) |
| SecretRef | vibecheck-secrets | OK |
| ConfigMapRef | vibecheck-common-config | OK |
| server.shutdown: graceful | Configured via ConfigMap env | VERIFY |

### 3.2 Missing from Service Deployments

- `securityContext` (runAsNonRoot, readOnlyRootFilesystem) -- not confirmed in templates
- `serviceAccountName` -- services likely use default SA
- Resource limits on initContainers

---

## 4. HPA Configuration Audit

| Service | minReplicas | maxReplicas | CPU Target | Memory Target |
|---|---|---|---|---|
| gateway-service | 2 (prod: 3) | 5 (prod: 10) | 70% (prod: 65%) | 80% (prod: 75%) |
| auth-service | 2 (prod: 3) | 4 (prod: 8) | 70% (prod: 65%) | -- |
| booking-service | 2 (prod: 4) | 6 (prod: 12) | 70% (prod: 60%) | 80% (prod: 70%) |

HPA behavior configured:
- scaleDown: stabilizationWindowSeconds=300, maxDecrease=20% per 60s
- scaleUp: stabilizationWindowSeconds=0, maxIncrease=100% or 4 pods per 15s

**Status:** HPA configuration is appropriate. Stabilization windows prevent oscillation.

---

## 5. PDB Configuration Audit

| Service | minAvailable | Configured In |
|---|---|---|
| gateway-service | 2 | values-prod.yaml |
| auth-service | 2 | values-prod.yaml |
| booking-service | 2 | values-prod.yaml |
| payment-service | 2 | values-prod.yaml |

**Status: PARTIAL** -- PDB only exists for prod values. movie-service, theatre-service, show-service, notification-service have NO PDB.

---

## 6. Network Policy Audit

Two NetworkPolicies deployed when networkPolicy.enabled=true:
1. `vibecheck-internal-traffic` -- allows all intra-namespace microservice communication
2. `vibecheck-gateway-ingress` -- allows external traffic to gateway only

**Status: PARTIAL** -- policies exist but are permissive (all intra-namespace traffic allowed). Database and Kafka access not restricted by NetworkPolicy.

---

## 7. Storage Configuration Audit

| Component | Size | StorageClass | AccessMode |
|---|---|---|---|
| MySQL | 5Gi (prod: same) | "" (cluster default) | ReadWriteOnce |
| Redis | 2Gi | "" (cluster default) | ReadWriteOnce |
| Kafka | 5Gi | "" (cluster default) | ReadWriteOnce |
| ZooKeeper | 2Gi | "" (cluster default) | ReadWriteOnce |

**Status: PARTIAL** -- default storage class is environment-dependent. No StorageClass ensures IO performance guarantees. No backup StorageClass for high-throughput Kafka workloads.

---

## 8. Secrets Configuration Audit

| Secret | Status |
|---|---|
| DB_ROOT_PASSWORD | IN GIT (values.yaml) |
| JWT_SECRET | IN GIT (values.yaml AND values-minikube.yaml) |
| INTERNAL_SECURITY_SECRET | IN GIT |
| PAYMENT_WEBHOOK_SECRET | IN GIT |
| RAZORPAY_KEY_ID | IN GIT |
| RAZORPAY_KEY_SECRET | IN GIT |
| STRIPE_API_KEY | IN GIT |
| STRIPE_WEBHOOK_SECRET | IN GIT |
| MAIL_PASSWORD | IN GIT |

**Status: FAIL** -- ALL production-sensitive secrets are committed to Git in values.yaml.

---

## 9. Observability Configuration

| Component | Status |
|---|---|
| Prometheus | Deployment in monitoring/ -- scrapes /actuator/prometheus |
| Grafana | Deployment with dashboard ConfigMap |
| Service annotations | prometheus.io/scrape: "true" on all services |
| Metrics export | MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED: "true" |
| Health probes | liveness/readiness states exposed |
| Tracing | Brave/Zipkin configured in application code |

**Status: PARTIAL** -- Loki and Jaeger present in Docker Compose but not in Helm chart.

---

## 10. Summary Scorecard

| Area | Status | Critical Issues |
|---|---|---|
| Helm lint | PASS | None |
| Helm template | PASS | None |
| Kafka HA | FAIL | Single broker, replication=1, hardcoded BROKER_ID |
| ZooKeeper HA | FAIL | Deployment (not StatefulSet), no persistence |
| MySQL HA | FAIL | Single replica, no managed DB strategy |
| Redis HA | FAIL | Single replica, no Sentinel/Cluster |
| Secret Management | FAIL | ALL secrets in Git |
| Service PDB | PARTIAL | Only 4 of 8 services have PDB in prod |
| HPA | PASS | Configured with appropriate windows |
| NetworkPolicy | PARTIAL | Permissive intra-namespace policy |
| Storage | PARTIAL | Default storage class, no performance guarantees |
| Observability | PARTIAL | Prometheus+Grafana OK; Loki/Jaeger only in Compose |
| Health Probes | PASS | All services have startup/readiness/liveness probes |
| Graceful Shutdown | PASS | terminationGracePeriodSeconds=60, preStop=10s |
| Rolling Update | PASS | maxUnavailable=0, maxSurge=25% |
