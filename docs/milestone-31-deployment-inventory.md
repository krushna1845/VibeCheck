# Milestone 31 — Deployment Inventory & Architecture Matrix

**Platform:** VibeCheck Movie Booking Platform  
**Auditor:** Principal Backend Engineer / SRE / Platform Engineer  
**Date:** 2026-09-28  
**Scope:** Complete repository inventory of deployable components, controllers, configurations, and network interfaces.

---

## 1. Executive Summary

This inventory catalogs every deployable microservice, stateful data tier, observability system, and deployment artifact in the VibeCheck platform. The platform is deployable via:
1. **Local Development:** `docker-compose.yml` (Docker Compose v2 bridge network).
2. **Kubernetes / Production:** `k8s/helm/vibecheck` Helm chart (Release 3.x, API version `v2`).

---

## 2. Deployable Services Inventory

| Service Name | Type | Base Image / Runtime | Port(s) | Replicas (Dev / Prod) | Health Probes (Liveness / Readiness) | Controller |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **gateway-service** | Stateless | `eclipse-temurin:21-jre` | `8079/TCP` | 2 / 3 | `/actuator/health/liveness`<br>`/actuator/health/readiness` | Deployment |
| **auth-service** | Stateless | `eclipse-temurin:21-jre` | `8080/TCP` | 2 / 3 | `/actuator/health/liveness`<br>`/actuator/health/readiness` | Deployment |
| **movie-service** | Stateless | `eclipse-temurin:21-jre` | `8081/TCP` | 2 / 2 | `/actuator/health/liveness`<br>`/actuator/health/readiness` | Deployment |
| **theatre-service** | Stateless | `eclipse-temurin:21-jre` | `8082/TCP` | 2 / 2 | `/actuator/health/liveness`<br>`/actuator/health/readiness` | Deployment |
| **show-service** | Stateless | `eclipse-temurin:21-jre` | `8083/TCP` | 2 / 2 | `/actuator/health/liveness`<br>`/actuator/health/readiness` | Deployment |
| **booking-service** | Stateless / Worker | `eclipse-temurin:21-jre` | `8084/TCP` | 2 / 4 | `/actuator/health/liveness`<br>`/actuator/health/readiness` | Deployment |
| **payment-service** | Stateless | `eclipse-temurin:21-jre` | `8085/TCP` | 2 / 3 | `/actuator/health/liveness`<br>`/actuator/health/readiness` | Deployment |
| **notification-service** | Stateless / Consumer | `eclipse-temurin:21-jre` | `8086/TCP` | 2 / 2 | `/actuator/health/liveness`<br>`/actuator/health/readiness` | Deployment |
| **frontend** | Static Web | Nginx / HTML5 SPA | `80/TCP` | 1 / 1 | Static index.html | Nginx / Ingress |

---

## 3. Stateful Infrastructure Inventory

| Component | Controller | Image / Tag | Mount Path | Storage Claim | Access Mode | Purpose |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **mysql** | `StatefulSet` | `mysql:8.0` | `/var/lib/mysql` | `5Gi` | `ReadWriteOnce` | Central relational store (8 isolated microservice schemas) |
| **redis** | `StatefulSet` | `redis:7-alpine` | `/data` | `2Gi` | `ReadWriteOnce` | Distributed seat locking, API rate limiting, cache |
| **kafka** | `StatefulSet` | `confluentinc/cp-kafka:7.5.0` | `/var/lib/kafka/data` | `5Gi` | `ReadWriteOnce` | Event streaming broker (booking events, payment events, DLTs) |
| **zookeeper** | `Deployment` | `confluentinc/cp-zookeeper:7.5.0` | `/var/lib/zookeeper/data` | `2Gi` | `ReadWriteOnce` | Cluster consensus for Kafka broker |

---

## 4. Observability & Monitoring Infrastructure

| Component | Controller | Image / Tag | Port | Scrape Targets / Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **prometheus** | `Deployment` | `prom/prometheus:v2.48.0` | `9090/TCP` | Scrapes `/actuator/prometheus` across all 8 microservices |
| **grafana** | `Deployment` | `grafana/grafana:10.2.0` | `3000/TCP` | Production dashboards for JVM, HTTP latency, HikariCP, Kafka lag |

---

## 5. Kubernetes Resource Manifests (`k8s/helm/vibecheck`)

```text
k8s/helm/vibecheck/
├── Chart.yaml                               # Chart metadata (v2, appVersion: 1.0.0)
├── values.yaml                              # Default values (staging / development)
├── values-prod.yaml                         # Production high-availability override
├── values-minikube.yaml                     # Minikube / local Kubernetes override
├── templates/
│   ├── _helpers.tpl                         # Common labels, selectors, and initContainer helpers
│   ├── namespace.yaml                       # Dedicated namespace (vibecheck)
│   ├── autoscaling/
│   │   ├── hpa.yaml                         # HorizontalPodAutoscalers with stabilization windows
│   │   └── pdb.yaml                         # PodDisruptionBudgets (minAvailable: 2)
│   ├── configmaps/
│   │   ├── common-config.yaml               # Shared Spring Boot, JVM, and Actuator variables
│   │   └── mysql-init-config.yaml           # Database creation & grant scripts
│   ├── infrastructure/
│   │   ├── mysql-statefulset.yaml           # MySQL 8.0 StatefulSet & PVC
│   │   ├── mysql-service.yaml               # MySQL ClusterIP Service (3306)
│   │   ├── redis-statefulset.yaml           # Redis 7.0 StatefulSet & AOF PVC
│   │   ├── redis-service.yaml               # Redis ClusterIP Service (6379)
│   │   ├── kafka-statefulset.yaml           # Kafka 7.5 StatefulSet & Log PVC
│   │   ├── kafka-service.yaml               # Kafka ClusterIP Service (9092, 29092)
│   │   ├── zookeeper-deployment.yaml        # Zookeeper single-replica deployment
│   │   └── zookeeper-service.yaml           # Zookeeper ClusterIP Service (2181)
│   ├── ingress/
│   │   └── ingress.yaml                     # Ingress controller routing to gateway-service
│   ├── monitoring/
│   │   ├── prometheus-configmap.yaml        # Scrape configs for all 8 services
│   │   ├── prometheus-deployment.yaml       # Prometheus single-replica deployment & Service
│   │   ├── grafana-configmap.yaml           # Datasource configuration
│   │   ├── grafana-dashboard-configmap.yaml # Pre-loaded Grafana JSON dashboards
│   │   └── grafana-deployment.yaml          # Grafana UI deployment & Service
│   ├── secrets/
│   │   └── vibecheck-secrets.yaml           # Opaque secrets (JWT, passwords, webhook tokens)
│   ├── security/
│   │   └── network-policy.yaml              # Zero-trust network segmentation
│   └── services/
│       ├── auth-service.yaml                # Auth service Deployment & Service
│       ├── booking-service.yaml             # Booking service Deployment & Service
│       ├── gateway-deployment.yaml          # Gateway service Deployment
│       ├── gateway-service.yaml             # Gateway service ClusterIP Service
│       ├── movie-service.yaml               # Movie service Deployment & Service
│       ├── notification-service.yaml        # Notification service Deployment & Service
│       ├── payment-service.yaml             # Payment service Deployment & Service
│       ├── show-service.yaml                # Show service Deployment & Service
│       └── theatre-service.yaml             # Theatre service Deployment & Service
```

---

## 6. Configuration Profiles Matrix

| Profile | Target Environment | Activation Mechanism | Key Differences |
| :--- | :--- | :--- | :--- |
| **`local`** | Local Docker Compose | `SPRING_PROFILES_ACTIVE: local` | Embedded dev configs, verbose logging, fast retry timeouts |
| **`prod`** | Kubernetes Production | `SPRING_PROFILES_ACTIVE: production` | Strict `ddl-auto: validate`, minimal logging, high-availability pools |
| **`observability`** | All environments | `spring.profiles.include: observability` | Actuator Prometheus exporter, OpenTelemetry trace sampling |
