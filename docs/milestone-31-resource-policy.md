# Milestone 31 — Kubernetes Resource Management & Capacity Policy

**Platform:** VibeCheck Movie Booking Platform  
**Auditor:** Principal Backend Engineer / SRE / Platform Engineer  
**Date:** 2026-09-28  
**Scope:** CPU/Memory requests and limits, JVM heap sizing, and Quality of Service (QoS) classification.

---

## 1. Executive Summary

Kubernetes resource management directly governs container scheduling, node packing density, Horizontal Pod Autoscaling (HPA) thresholds, and Out-of-Memory (OOM) killer eviction order. 

This policy establishes strict, evidence-based CPU and memory boundaries across all 8 microservices and 3 stateful components to eliminate:
1. **Unbounded noisy-neighbor contention:** Preventing an unconstrained container from starving adjacent services of CPU cycles.
2. **Premature OOMKills:** Aligning the container memory limit with the Spring Boot JVM heap (`-Xmx220m`) and native memory overhead (Metaspace, thread stacks, Netty off-heap buffers).
3. **CPU Throttling:** Providing sufficient burst headroom in limits to prevent latency spikes during JIT compilation and garbage collection pauses.

---

## 2. Resource Allocation Matrix

### 2.1 Stateless Microservices (Default Staging vs Production Overrides)

| Microservice | Staging Requests (CPU / Mem) | Staging Limits (CPU / Mem) | Production Requests (CPU / Mem) | Production Limits (CPU / Mem) | Kubernetes QoS Class |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **gateway-service** | `200m / 384Mi` | `800m / 768Mi` | `500m / 768Mi` | `2000m / 1536Mi` | Burstable |
| **auth-service** | `200m / 384Mi` | `800m / 768Mi` | `400m / 512Mi` | `1500m / 1024Mi` | Burstable |
| **movie-service** | `150m / 384Mi` | `600m / 640Mi` | `300m / 512Mi` | `1000m / 1024Mi` | Burstable |
| **theatre-service** | `150m / 384Mi` | `600m / 640Mi` | `300m / 512Mi` | `1000m / 1024Mi` | Burstable |
| **show-service** | `200m / 384Mi` | `800m / 768Mi` | `400m / 512Mi` | `1500m / 1024Mi` | Burstable |
| **booking-service** | `250m / 512Mi` | `1000m / 1024Mi`| `500m / 1024Mi` | `2500m / 2048Mi` | Burstable |
| **payment-service** | `150m / 384Mi` | `600m / 640Mi` | `400m / 512Mi` | `1500m / 1024Mi` | Burstable |
| **notification-service** | `150m / 384Mi` | `600m / 640Mi` | `300m / 512Mi` | `1000m / 1024Mi` | Burstable |

### 2.2 Stateful Infrastructure Components

| Component | Requests (CPU / Mem) | Limits (CPU / Mem) | PVC Storage | Storage Access Mode | QoS Class |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **mysql (8.0)** | `250m / 512Mi` | `1000m / 1Gi` | `5Gi` | `ReadWriteOnce` | Burstable |
| **redis (7-alpine)**| `100m / 128Mi` | `500m / 512Mi` | `2Gi` | `ReadWriteOnce` | Burstable |
| **kafka (7.5.0)** | `250m / 512Mi` | `1000m / 1Gi` | `5Gi` | `ReadWriteOnce` | Burstable |
| **zookeeper (7.5.0)**| `150m / 256Mi` | `500m / 512Mi` | `2Gi` | `ReadWriteOnce` | Burstable |

---

## 3. JVM Memory Sizing & Containment Math

In `k8s/helm/vibecheck/templates/configmaps/common-config.yaml`, the JVM options for all microservices are centrally tuned:

```bash
JAVA_TOOL_OPTIONS: "-Xms96m -Xmx220m -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
```

### 3.1 Memory Breakdown Analysis
Total Container Memory Consumption = Heap + Metaspace + Thread Stacks + Off-Heap Native:
* **Max Heap (`-Xmx220m`):** `220 MiB`
* **Metaspace (Spring Boot classes + Jackson + Hibernate):** `~80 MiB`
* **Thread Stacks (Tomcat/Netty worker threads, 1MB each):** `~40 MiB`
* **Direct Buffers / Cgroups / Native overhead:** `~44 MiB`
* **Total Estimated Working Set:** **`384 MiB`**

### 3.2 Safety Margin
* Minimum container memory request is set to **`384 MiB`** (or `512 MiB` for `booking-service` which runs background outbox batches).
* Container memory limit is set to **`640 MiB - 1024 MiB`** in staging and **`1024 MiB - 2048 MiB`** in production.
* **Result:** The container limit provides a 100%–200% headroom above maximum JVM heap usage, ensuring Linux cgroup OOM killer will never terminate healthy Java processes under traffic spikes.

---

## 4. CPU Management & Throttling Mitigation

1. **CFS Quota Throttling Avoidance:**
   * Spring Boot applications on Java 21 utilize C2 JIT compilation which consumes significant CPU during startup and initial traffic warm-up.
   * By allocating at least `150m–500m` request and allowing burst limits up to `1000m–2500m`, JIT warmup finishes rapidly without causing CFS scheduler throttling.
2. **HorizontalPodAutoscaler Baseline:**
   * Kubernetes HPA utilizes container CPU/Memory **requests** (not limits) as the denominator for percentage calculations (`targetCPUUtilizationPercentage: 65% - 70%`).
   * Because every deployment defines explicit CPU requests, HPA metric calculation operates reliably without scaling stalls.
