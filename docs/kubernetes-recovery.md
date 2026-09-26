# Kubernetes Recovery & Operational Resilience — VibeCheck Platform

**Document Version:** 1.0  
**Milestone:** 31 — Production Operations & Disaster Recovery  
**Date:** 2026-09-26  
**Chart:** `k8s/helm/vibecheck`

---

## 1. Architecture Overview: Local vs. Kubernetes

It is critical to distinguish between the two execution environments used by VibeCheck:

| Dimension | Local / Development (`docker-compose.yml`) | Kubernetes / Production (`k8s/helm/vibecheck`) |
| :--- | :--- | :--- |
| **Orchestrator** | Docker Compose v2 | Kubernetes 1.28+ (Helm 3.x) |
| **Stateful Services** | Single-container MySQL, Redis, Kafka, Zookeeper | StatefulSets with PersistentVolumeClaims (PVCs) |
| **Stateless Services** | Single-container microservices (8 services) | Multi-replica Deployments (2–3 replicas per service in prod) |
| **Persistence** | Named Docker volumes (`mysql_data`, `redis_data`, `kafka_data`) | Dynamic PV provisioning via StorageClasses (`ReadWriteOnce`) |
| **Startup Ordering** | `depends_on: { service: { condition: service_healthy } }` | K8s initContainers (`waitForMySQL`, `waitForKafka`) |
| **Health Checks** | Docker `healthcheck` (curl/mysqladmin/redis-cli) | Liveness, Readiness, and Startup Probes |
| **Graceful Shutdown** | Docker default stop signal (SIGTERM, default 10s) | Pod `preStop` hook (10s) + Spring graceful shutdown (30s) + `terminationGracePeriodSeconds` (45–60s) |
| **High Availability** | None (single point of failure per component) | PodAntiAffinity, HorizontalPodAutoscaler, PodDisruptionBudgets |
| **Service Discovery** | Docker internal DNS bridge network (`vibecheck-network`) | Kubernetes CoreDNS ClusterIP Services (`<service>.vibecheck.svc.cluster.local`) |

---

## 2. Pod Lifecycle and Graceful Termination

### 2.1 The Termination Sequence in Kubernetes
When a pod is evicted, scaled down, or updated during a rolling deployment, the following sequence occurs:

1. **Endpoint Removal:** Kubernetes removes the Pod IP from the Service endpoints list. No new traffic is routed to this pod.
2. **`preStop` Hook Execution:** A `sleep 10` hook runs inside the container:
   ```yaml
   lifecycle:
     preStop:
       exec:
         command: ["/bin/sh", "-c", "sleep 10"]
   ```
   This 10-second delay guarantees that iptables / kube-proxy / ingress controllers across all cluster nodes have propagated the endpoint removal before the application begins shutting down.
3. **SIGTERM Delivery:** Kubernetes sends SIGTERM to the Spring Boot application (PID 1).
4. **Spring Graceful Shutdown:**
   - Spring Boot ceases accepting new HTTP requests on its port.
   - Spring Boot waits up to `spring.lifecycle.timeout-per-shutdown-phase: 30s` for in-flight requests and ongoing outbox relays / payment webhooks to finish cleanly.
   - Kafka message listeners stop polling and commit active offsets.
5. **SIGKILL Deadline (`terminationGracePeriodSeconds`):**
   - If the pod has not exited after `terminationGracePeriodSeconds`, the kubelet issues SIGKILL.
   - **Configured values in VibeCheck:**
     - `booking-service`: **60s** (10s preStop + 30s Spring shutdown + 20s safety buffer)
     - `payment-service`: **60s** (10s preStop + 30s Spring shutdown + 20s safety buffer)
     - `notification-service`: **60s** (10s preStop + 30s Spring shutdown + 20s safety buffer)
     - `gateway-service`: **45s** (10s preStop + 30s Spring shutdown + 5s buffer)
     - Standard stateless services: **30s** (Kubernetes default)

---

## 3. Health Probes Configuration

All VibeCheck Spring Boot services expose Actuator endpoints utilized by Kubernetes probes:

```yaml
livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: http
  initialDelaySeconds: 45
  periodSeconds: 15
  timeoutSeconds: 5
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: http
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3
```

- **Liveness Probe:** Fails only when internal JVM state is deadlocked or unrecoverable. Kubelet restarts the container if 3 consecutive checks fail.
- **Readiness Probe:** Fails if the service cannot reach its required downstream datastores (MySQL/Kafka/Redis). When failing, the pod is temporarily removed from service routing without being destroyed.

---

## 4. Stateful Infrastructure: StatefulSets & PVCs

The platform provisions three stateful components via Helm templates in `templates/infrastructure/`:

### 4.1 MySQL StatefulSet (`mysql-statefulset.yaml`)
- **Controller:** `StatefulSet` with `replicas: 1`
- **VolumeClaimTemplate:**
  - `accessModes: [ "ReadWriteOnce" ]`
  - Mount path: `/var/lib/mysql`
  - Default size: `5Gi` (configurable in `values.yaml`)
- **Init ConfigMap:** Mounts `mysql-init-config.yaml` to initialize the 8 microservice databases and permissions on first launch.
- **Recovery Behavior:** If the MySQL pod crashes, the StatefulSet controller reattaches the existing PV with zero data loss. Pod re-initialization skips table creation if data directories already exist.

### 4.2 Redis StatefulSet (`redis-statefulset.yaml`)
- **Controller:** `StatefulSet` with `replicas: 1`
- **VolumeClaimTemplate:**
  - `accessModes: [ "ReadWriteOnce" ]`
  - Mount path: `/data`
  - Default size: `2Gi`
- **Persistence:** Append-Only File (`appendonly yes`) enabled via startup arguments:
  ```yaml
  command: ["redis-server", "--appendonly", "yes", "--dir", "/data"]
  ```
- **Recovery Behavior:** On pod recreation, Redis loads `/data/appendonly.aof` to restore non-expired keys and rate limits.

### 4.3 Kafka StatefulSet (`kafka-statefulset.yaml`)
- **Controller:** `StatefulSet` with `replicas: 1`
- **VolumeClaimTemplate:**
  - Mount path: `/var/lib/kafka/data`
  - Default size: `5Gi`
- **Recovery Behavior:** Broker ID is fixed (`KAFKA_BROKER_ID: 1`). On pod recreation, Kafka connects to Zookeeper, detects existing log segments on the re-mounted PVC, recovers committed offset topics (`__consumer_offsets`), and resumes event streaming without topic deletion.

---

## 5. Dependency Management: Init Containers

Microservices depend on MySQL and Kafka. When deploying the entire cluster simultaneously, race conditions can occur if services attempt Flyway migrations before MySQL is accepting connections.

VibeCheck solves this via standard Helm initContainer templates (`_helpers.tpl`):

```yaml
{{- define "vibecheck.waitForMySQL" -}}
- name: wait-for-mysql
  image: busybox:1.36
  command: ['sh', '-c', 'until nc -z -w 2 {{ .Values.infrastructure.mysql.name }} 3306; do echo waiting for mysql; sleep 2; done']
{{- end -}}

{{- define "vibecheck.waitForKafka" -}}
- name: wait-for-kafka
  image: busybox:1.36
  command: ['sh', '-c', 'until nc -z -w 2 {{ .Values.infrastructure.kafka.name }} 9092; do echo waiting for kafka; sleep 2; done']
{{- end -}}
```

---

## 6. High Availability & Disruption Protection

### 6.1 Pod Anti-Affinity
Enabled in `values-prod.yaml` (`podAntiAffinity.enabled: true`). Soft anti-affinity (`preferredDuringSchedulingIgnoredDuringExecution`) ensures pods of the same service are scheduled on separate Kubernetes worker nodes where possible.

### 6.2 PodDisruptionBudgets (PDB)
Defined in `templates/autoscaling/pdb.yaml`. For multi-replica production deployments (`values-prod.yaml`):
- `gateway-service`: `minAvailable: 2` (out of 3)
- `booking-service`: `minAvailable: 2` (out of 3)
- `payment-service`: `minAvailable: 2` (out of 3)
- `auth-service`: `minAvailable: 2` (out of 3)

This guarantees that voluntary node maintenance, cluster upgrades, or autoscaling drain operations will never terminate all replicas of a critical service simultaneously.

---

## 7. Disaster Recovery Procedures in Kubernetes

### 7.1 Pod CrashLoopBackOff Recovery
1. Check pod status:
   ```bash
   kubectl get pods -n vibecheck
   ```
2. Inspect container termination reason and logs:
   ```bash
   kubectl describe pod <pod-name> -n vibecheck
   kubectl logs <pod-name> -n vibecheck --previous
   ```
3. Common cause: MySQL connection failure. Verify MySQL service health:
   ```bash
   kubectl get pods -l app.kubernetes.io/name=mysql -n vibecheck
   ```

### 7.2 Storage / PVC Recovery
If a node fails and a StatefulSet pod is stuck in `ContainerCreating` waiting for volume detachment:
```bash
# Check VolumeAttachment
kubectl get volumeattachment
# Force delete terminating pod if safe (node confirmed dead)
kubectl delete pod mysql-0 -n vibecheck --force --grace-period=0
```

### 7.3 Restoring MySQL Database Inside Kubernetes
To restore a backup file into the Kubernetes MySQL StatefulSet:
```bash
# 1. Uncompress and stream backup into mysql-0 pod
zcat vibecheck_mysql_backup_20260926_120000.sql.gz | kubectl exec -i mysql-0 -n vibecheck -- \
  mysql -u root -p"$DB_ROOT_PASSWORD"

# 2. Restart microservice pods to re-initialize connection pools
kubectl rollout restart deployment -n vibecheck -l app.kubernetes.io/part-of=vibecheck
```
