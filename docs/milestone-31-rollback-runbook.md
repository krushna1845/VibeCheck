# Milestone 31 — Production Deployment Rollback Runbook

**Runbook ID:** RB-OPS-ROLLBACK-001  
**Target:** VibeCheck Microservices Kubernetes Deployments  
**Auditor:** Principal Backend Engineer / SRE / Platform Engineer  
**Date:** 2026-09-28  
**Scope:** Immediate emergency mitigation and non-destructive deployment rollback procedures.

---

## 1. When to Initiate Rollback

Initiate rollback immediately if any of the following occur within 15 minutes of a new deployment:
1. `HighHttp5xxRate` or `BookingFailureSpike` alert fires.
2. New pods enter `CrashLoopBackOff` or repeatedly fail readiness probes.
3. In-flight transactions experience widespread serialization errors or unhandled exceptions.
4. p99 latency degrades by > 200% compared to baseline.

> [!CAUTION]
> **CRITICAL RULE: DO NOT ATTEMPT DESTRUCTIVE DATABASE ROLLBACK**
> Never run automated reverse SQL migrations or execute `DROP TABLE` / `DROP COLUMN` during an active incident. In accordance with [`docs/milestone-31-database-deployment.md`](file:///c:/Users/acer/Downloads/booking-system/docs/milestone-31-database-deployment.md), all database migrations follow the expand/contract pattern and are backward-compatible. Rolling back application code to the previous container image is safe and will run against the updated database schema without issues.

---

## 2. Step-by-Step Rollback Procedure

```
                     ┌───────────────────────────────┐
                     │   Incident Detected / Alert   │
                     └───────────────┬───────────────┘
                                     │
                                     ▼
                     ┌───────────────────────────────┐
                     │ Step 1: Pause / Stop Rollout  │
                     └───────────────┬───────────────┘
                                     │
                                     ▼
                     ┌───────────────────────────────┐
                     │ Step 2: Rollback Deployment   │
                     │    (kubectl rollout undo)     │
                     └───────────────┬───────────────┘
                                     │
                                     ▼
                     ┌───────────────────────────────┐
                     │ Step 3: Verify Readiness      │
                     │    (Probes & Traffic Drain)   │
                     └───────────────┬───────────────┘
                                     │
                                     ▼
                     ┌───────────────────────────────┐
                     │ Step 4: Verify Kafka & DB     │
                     │    (Consumers & Outbox Flow)  │
                     └───────────────┬───────────────┘
                                     │
                                     ▼
                     ┌───────────────────────────────┐
                     │ Step 5: Smoke Test & Signoff  │
                     │    (Synthetic E2E Flow)       │
                     └───────────────────────────────┘
```

---

### Step 1: Immediately Pause the Failing Rollout
Prevent Kubernetes from spinning up further unhealthy pods:
```bash
# Example: pausing a failing booking-service rollout
kubectl rollout pause deployment/booking-service -n vibecheck
```

### Step 2: Inspect Failing Pod Logs for Diagnostic Evidence
Before rolling back, capture logs to avoid losing transient crash diagnostics:
```bash
# Capture recent failure logs
kubectl logs -n vibecheck -l app.kubernetes.io/name=booking-service --tail=200 > crash_logs_$(date +%s).log

# Inspect deployment rollout history
kubectl rollout history deployment/booking-service -n vibecheck
```

### Step 3: Execute Deployment Rollback
Roll back the deployment controller to the previous known-good revision:
```bash
# Undo rollout to immediate prior revision
kubectl rollout undo deployment/booking-service -n vibecheck

# Or undo to a specific revision number if multiple releases were attempted
# kubectl rollout undo deployment/booking-service --to-revision=3 -n vibecheck

# Monitor rollback progress until all replicas are healthy
kubectl rollout status deployment/booking-service -n vibecheck --timeout=120s
```

### Step 4: Verify Health & Readiness Probes
Confirm that all restored pods are healthy and receiving ingress traffic:
```bash
# Check pod running state and ready count (should be 2/2 or 4/4)
kubectl get pods -n vibecheck -l app.kubernetes.io/name=booking-service -o wide

# Check Actuator health endpoint via port-forward or curl
kubectl exec -it deployment/gateway-service -n vibecheck -- \
  curl -s http://booking-service:8084/actuator/health/readiness
# Expected output: {"status":"UP"}
```

### Step 5: Verify Kafka Consumers & Outbox Relay
Ensure message streaming resumed and consumers did not crash during the revision switch:
```bash
# Verify consumer group lag is decreasing
kubectl exec -it kafka-0 -n vibecheck -- \
  kafka-consumer-groups --bootstrap-server localhost:9092 --describe --group notification-service-group

# Check that Outbox relay is publishing events
kubectl exec -it mysql-0 -n vibecheck -- mysql -uroot -p"$DB_ROOT_PASSWORD" -e \
  "SELECT status, count(*) FROM vibecheck_booking.outbox_events GROUP BY status;"
```

### Step 6: Execute Synthetic End-to-End Verification
Run a synthetic health flow through the API gateway:
```bash
# 1. Health check gateway
curl -i http://localhost:8079/actuator/health

# 2. Browse movie catalog
curl -s http://localhost:8079/api/v1/movies | jq .

# 3. Check booking service responds
curl -i http://localhost:8079/api/v1/bookings/health
```

### Step 7: Incident Escalation & Postmortem
1. Notify stakeholders on Slack `#incident-management` that rollback completed and service is restored.
2. File a P1 postmortem ticket containing:
   - Root cause identified from captured crash logs.
   - Exact commit SHA and container image digest reverted.
   - Customer impact duration and estimated failed booking attempts.
