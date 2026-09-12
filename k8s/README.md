# VibeCheck — Kubernetes & Helm Orchestration Platform (Milestone 23)

## Overview
This directory contains the production-grade **Kubernetes** and **Helm v3/v4** orchestration assets for the **VibeCheck** distributed movie booking system.

### Architecture

```
                    Internet / External Clients
                               │
                               ▼
               ┌───────────────────────────────┐
               │    NGINX Ingress Controller   │
               └───────────────┬───────────────┘
                               │ (vibecheck.local:80)
                               ▼
               ┌───────────────────────────────┐
               │    gateway-service (HPA)      │
               │         (Port 8079)           │
               └───────────────┬───────────────┘
                               │ Perimeter JWT Auth / Routing
         ┌─────────────────────┼──────────────────────┐
         ▼                     ▼                      ▼
  ┌──────────────┐      ┌──────────────┐      ┌──────────────┐
  │ auth-service │      │movie-service │      │theatre-serv. │
  │ (Port 8080)  │      │ (Port 8081)  │      │ (Port 8082)  │
  └──────┬───────┘      └──────┬───────┘      └──────┬───────┘
         │                     │                      │
         ├─────────────────────┴──────────────────────┤
         ▼                                            ▼
  ┌──────────────┐                             ┌──────────────┐
  │ show-service │                             │booking-serv. │ (HPA)
  │ (Port 8083)  │                             │ (Port 8084)  │
  └──────┬───────┘                             └──────┬───────┘
         │                                            │
         ├────────────────────────────────────────────┼──────────────┐
         ▼                                            ▼              ▼
  ┌──────────────┐                             ┌──────────────┐┌──────────────┐
  │payment-serv. │                             │notification  ││  Redis 7     │
  │ (Port 8085)  │                             │ (Port 8086)  ││  StatefulSet │
  └──────┬───────┘                             └──────┬───────┘└──────────────┘
         │                                            │
         ▼                                            ▼
  ┌──────────────┐                             ┌──────────────┐
  │   MySQL 8    │                             │Apache Kafka  │
  │ StatefulSet  │                             │ StatefulSet  │
  └──────────────┘                             └──────────────┘
```

---

## Directory Structure

```
k8s/
├── helm/
│   └── vibecheck/
│       ├── Chart.yaml                     # Helm chart metadata
│       ├── values.yaml                     # Default production-ready values
│       ├── values-minikube.yaml            # Single-node Minikube overrides
│       ├── values-prod.yaml                # Multi-node HA production overrides
│       └── templates/
│           ├── _helpers.tpl                # Standard labels and naming helpers
│           ├── namespace.yaml              # Dedicated vibecheck namespace
│           ├── configmaps/
│           │   ├── common-config.yaml      # Shared application configuration
│           │   └── mysql-init-config.yaml  # Multi-database init schema script
│           ├── secrets/
│           │   └── vibecheck-secrets.yaml  # Credentials, JWT, DB passwords
│           ├── infrastructure/
│           │   ├── mysql-statefulset.yaml  # MySQL 8.0 with PVC
│           │   ├── mysql-service.yaml
│           │   ├── redis-statefulset.yaml  # Redis 7 with AOF persistence & PVC
│           │   ├── redis-service.yaml
│           │   ├── zookeeper-deployment.yaml
│           │   ├── zookeeper-service.yaml
│           │   ├── kafka-statefulset.yaml  # Kafka broker with PVC
│           │   └── kafka-service.yaml
│           ├── services/
│           │   ├── gateway-deployment.yaml # Gateway with Actuator health probes
│           │   ├── gateway-service.yaml
│           │   ├── auth-service.yaml       # Auth Deployment + ClusterIP Service
│           │   ├── movie-service.yaml      # Movie Deployment + ClusterIP Service
│           │   ├── theatre-service.yaml    # Theatre Deployment + ClusterIP Service
│           │   ├── show-service.yaml       # Show Deployment + ClusterIP Service
│           │   ├── booking-service.yaml    # Booking Deployment + ClusterIP Service
│           │   ├── payment-service.yaml    # Payment Deployment + ClusterIP Service
│           │   └── notification-service.yaml # Notification Deployment + ClusterIP Service
│           ├── ingress/
│           │   └── ingress.yaml            # Ingress rules routing / to gateway
│           ├── autoscaling/
│           │   └── hpa.yaml                # HorizontalPodAutoscalers for gateway & booking
│           └── security/
│               └── network-policy.yaml     # Zero-trust microservice isolation
└── scripts/
    ├── deploy-minikube.ps1                 # Automated local deploy script
    ├── smoke-test-k8s.ps1                  # Cluster health and API smoke tests
    └── undeploy-minikube.ps1               # Automated teardown script
```

---

## Quick Start on Minikube

### Prerequisites
- **Docker Desktop** (running)
- **Minikube** (`v1.38+`)
- **kubectl** (`v1.36+`)
- **helm** (`v4+` or `v3+`)

### 1. One-Click Automated Deployment
Run the PowerShell script from the repository root:
```powershell
.\k8s\scripts\deploy-minikube.ps1
```

The script will automatically:
1. Ensure Minikube is started (`--driver=docker --cpus=4 --memory=8192`).
2. Enable `ingress` and `metrics-server` addons.
3. Cache/load the 8 microservice Docker images into Minikube.
4. Deploy the Helm release `vibecheck` into the `vibecheck` namespace.
5. Wait for all StatefulSets and Deployments to reach `Ready` status.

### 2. Manual Helm Commands (Alternative)
```powershell
# Lint chart
helm lint k8s/helm/vibecheck

# Deploy chart
helm upgrade --install vibecheck ./k8s/helm/vibecheck `
    -f ./k8s/helm/vibecheck/values-minikube.yaml `
    --create-namespace `
    -n vibecheck
```

---

## Accessing the Cluster

### Option A: Via Minikube Ingress
1. Find your Minikube IP:
   ```powershell
   minikube ip
   ```
2. Add an entry to your `hosts` file (`C:\Windows\System32\drivers\etc\hosts`):
   ```
   <MINIKUBE_IP>   vibecheck.local
   ```
3. Access the system at `http://vibecheck.local`.

### Option B: Via Port-Forwarding (Instant)
```powershell
kubectl port-forward svc/gateway-service 8079:8079 -n vibecheck
```
Access the system at `http://localhost:8079`.

---

## Verification & Smoke Testing

Run the automated smoke test script while port-forwarding is active:
```powershell
.\k8s\scripts\smoke-test-k8s.ps1 -BaseUrl "http://localhost:8079"
```

The test validates:
- [x] Gateway Actuator Health (`/actuator/health` -> `UP`)
- [x] User Registration via Gateway (`POST /auth/register`)
- [x] User Authentication & JWT acquisition (`POST /auth/login`)
- [x] Gateway JWT routing to downstream protected services (`GET /movies`)
- [x] Cluster Pod health (all pods in `vibecheck` namespace `Running` and `Ready`)

---

## Production Deployment

To deploy in a multi-node Kubernetes production cluster:
```powershell
helm upgrade --install vibecheck ./k8s/helm/vibecheck `
    -f ./k8s/helm/vibecheck/values-prod.yaml `
    --create-namespace `
    -n vibecheck
```

Production features enabled:
- **Zero-Downtime Rolling Updates**: `maxSurge: 25%`, `maxUnavailable: 0`.
- **Pod Anti-Affinity**: Spreads pods across separate physical nodes.
- **Horizontal Pod Autoscaling**: Automatically scales gateway, auth, and booking pods under load.
- **NetworkPolicies**: Strict ingress controls preventing unauthorized traffic between pods.
- **Graceful Termination**: `preStop` sleep hook to allow in-flight connections to drain safely.

---

## Teardown
```powershell
.\k8s\scripts\undeploy-minikube.ps1
```
