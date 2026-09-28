# Milestone 31 — High Availability, Autoscaling & Disruption Policy

**Platform:** VibeCheck Movie Booking Platform  
**Auditor:** Principal Backend Engineer / SRE / Platform Engineer  
**Date:** 2026-09-28  
**Scope:** HorizontalPodAutoscaler (HPA), PodDisruptionBudgets (PDB), Rolling Updates, and Anti-Affinity.

---

## 1. Executive Summary

High Availability (HA) in VibeCheck ensures continuous platform operation across planned disruptions (node drains, cluster upgrades, rolling deployments) and unplanned disruptions (hardware node crashes, network partitions, container OOMs).

This policy establishes the formal boundaries for horizontal scaling, disruption budgets, and anti-affinity scheduling across both stateless microservices and stateful datastores.

---

## 2. Horizontal Pod Autoscaler (HPA) Architecture

Defined in `k8s/helm/vibecheck/templates/autoscaling/hpa.yaml` utilizing Kubernetes `autoscaling/v2`:

### 2.1 Services That Scale Horizontally

| Service | Staging Replicas (Min/Max) | Prod Replicas (Min/Max) | CPU Target | Memory Target | Scaling Rationale |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **gateway-service** | 2 / 5 | 3 / 10 | 65% | 75% | Ingress traffic entry point; scales with concurrent HTTP request volume. |
| **booking-service** | 2 / 6 | 4 / 12 | 60% | 70% | Core transactional engine; scales with concurrent ticket purchase spikes. |
| **auth-service** | 2 / 4 | 3 / 8 | 65% | — | Cryptographic JWT signing and bcrypt validation; scales under login load. |
| **payment-service** | 2 / 4 | 3 / 8 | 65% | — | Scales with external payment redirect & webhook ingestion volume. |
| **movie-service** | 2 / 4 (Prod) | 2 / 6 | 70% | — | Read-heavy catalog queries. |
| **theatre-service** | 2 / 4 (Prod) | 2 / 6 | 70% | — | Read-heavy auditorium queries. |
| **show-service** | 2 / 4 (Prod) | 2 / 6 | 70% | — | Read-heavy showtime & layout queries. |

### 2.2 Stabilization Windows & Flapping Prevention
To prevent oscillatory flapping (scaling up on a temporary spike and immediately down):
```yaml
behavior:
  scaleDown:
    stabilizationWindowSeconds: 300   # 5-minute cooldown before shedding replicas
    policies:
      - type: Percent
        value: 20                     # Shed at most 20% of capacity per minute
        periodSeconds: 60
  scaleUp:
    stabilizationWindowSeconds: 0     # Immediate reaction to traffic spikes
    policies:
      - type: Percent
        value: 100                    # Double capacity in 15 seconds if needed
        periodSeconds: 15
      - type: Pods
        value: 4                      # Or add at least 4 pods immediately
        periodSeconds: 15
    selectPolicy: Max
```

### 2.3 Services That Must NOT Scale Horizontally (Stateful Exclusions)
* **MySQL:** Single primary instance. Adding pod replicas without MySQL Group Replication or Galera cluster consensus will cause split-brain data corruption.
* **Redis:** Single primary instance. Horizontal scaling requires Redis Cluster topology or Redis Sentinel read-replicas.
* **Kafka:** Deployed as a single broker StatefulSet. Increasing `replicas` requires topic repartitioning and broker ID orchestration.

---

## 3. Pod Disruption Budgets (PDB)

Defined in `k8s/helm/vibecheck/templates/autoscaling/pdb.yaml` using `policy/v1`:

### 3.1 Production PDB Configurations (`values-prod.yaml`)

```yaml
gateway-service:    minAvailable: 2  (out of 3+ replicas)
booking-service:    minAvailable: 2  (out of 4+ replicas)
auth-service:       minAvailable: 2  (out of 3+ replicas)
payment-service:    minAvailable: 2  (out of 3+ replicas)
```

### 3.2 Operational Guarantees
1. **Node Maintenance (`kubectl drain`):** When an SRE drains a Kubernetes worker node, the eviction controller checks the PDB. It evicts only 1 pod at a time, ensuring that at least `minAvailable: 2` pods are always healthy and serving traffic.
2. **Rolling Deployments:** With `maxUnavailable: 0` and `maxSurge: 25%`, Kubernetes creates new ready pods before terminating old pods. PDB guarantees that voluntary cluster events never compound deployment rollouts into a complete service blackout.

---

## 4. Pod Scheduling & Anti-Affinity

All microservice deployments include soft pod anti-affinity in `values.yaml` (`podAntiAffinity.enabled: true`):

```yaml
affinity:
  podAntiAffinity:
    preferredDuringSchedulingIgnoredDuringExecution:
      - weight: 100
        podAffinityTerm:
          labelSelector:
            matchExpressions:
              - key: app.kubernetes.io/name
                operator: In
                values:
                  - booking-service
          topologyKey: kubernetes.io/hostname
```

* **Rationale:** The Kubernetes scheduler attempts to place replicas of the same service on separate physical worker nodes. If a hardware node crashes, only 1 replica is lost while the remaining replicas continue handling traffic without user interruption.

---

## 5. Behavior During Common Failure Scenarios

| Scenario | System Reaction | User / Business Impact |
| :--- | :--- | :--- |
| **Pod Crash (OOM / Panic)** | Kubelet detects exit, initiates `restartPolicy: Always`. Traffic routed to surviving replica(s). | Zero downtime (surviving replica handles in-flight load). |
| **Node Drain (Maintenance)** | PDB blocks node drain until new pod is scheduled on another node and passes readiness probe. | Zero downtime. |
| **Rolling Deployment** | New pod starts -> initContainers wait for MySQL/Kafka -> startupProbe passes -> readinessProbe passes -> old pod receives SIGTERM + preStop sleep (10s) -> in-flight requests finish (30s). | Zero downtime, zero dropped connections. |
| **MySQL Brief Interruption** | Spring Boot readiness probe fails -> pod marked Unready -> removed from endpoints. Liveness probe stays healthy -> pod is NOT killed. Re-added to endpoints once DB responds. | Temporary HTTP 503 until DB recovers; no cascading container restart storm. |
| **Kafka Broker Outage** | Outbox relay pauses and retries with backoff. Web request flow succeeds (booking created in DB). Once Kafka recovers, outbox drains backlog. | Zero dropped orders; asynchronous notifications delayed until broker recovers. |
