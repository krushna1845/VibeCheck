# Milestone 32 — Load Test Report

**Date:** 2026-09-28
**Platform:** VibeCheck Movie Booking Platform
**Milestone:** 32 — Production Infrastructure HA & DR Validation

---

## Executive Summary

The Milestone 31 audit CONDITIONAL verdict identified that the existing k6 load test suite
had never been executed against a live multi-replica staging cluster.

This report documents the k6 test inventory, test design review, and execution status.

**Load test execution against a live multi-replica staging cluster: NOT EXECUTED**

Reason: No multi-replica Kubernetes staging cluster is available in this environment.
All local testing is constrained to Docker Compose single-node topology.

All test scenarios are designed, documented, and ready for execution when a staging cluster is provisioned.

---

## 1. Existing k6 Test Inventory

### 1.1 Primary Test: seat-concurrency-stress-test.js

Location: `tests/k6/seat-concurrency-stress-test.js`

**Scenarios:**

| Scenario | Executor | VUs | Iterations | Duration | Purpose |
|---|---|---|---|---|---|
| browsing_traffic | ramping-vus | 10-100 | N/A | 2 min | Catalog browse load |
| hot_seat_race | shared-iterations | 100 | 500 | 2 min max | Seat lock contention |

**Thresholds:**
- `http_req_duration p(95) < 600ms` -- 95th percentile response time
- `unexpected_errors count == 0` -- Zero unhandled 5xx errors
- `successful_bookings count <= 5` -- Seat allocation bounded by seat count

**Custom Metrics:**
- `successful_bookings` (Counter)
- `seat_conflicts_rejected` (Counter)
- `unexpected_errors` (Counter)
- `booking_latency_ms` (Trend)

**Configurable via ENV:**
- `GATEWAY_URL` (default: http://127.0.0.1:8079)
- `SHOW_ID` (target show UUID)
- `SEAT_ID` (target seat UUID)

---

## 2. Load Test Scenarios (Planned)

### LOAD-01: Baseline
- VUs: 5
- Duration: 60 seconds
- Target: Verify all endpoints return 200, p95 < 200ms
- Status: NOT EXECUTED

### LOAD-02: Moderate Traffic
- VUs: 25 (browsing) + 25 (bookings)
- Duration: 2 minutes
- Target: p95 < 400ms, error rate < 1%
- Status: NOT EXECUTED

### LOAD-03: High Concurrency
- VUs: 100 (browsing) + 100 (bookings)
- Duration: 2 minutes
- Target: p95 < 600ms, error rate < 2%
- Status: NOT EXECUTED

### LOAD-04: Booking Contention (Existing hot_seat_race)
- VUs: 100
- Iterations: 500
- Target: exactly N seats booked (N = available seats), all others conflict-rejected
- Status: NOT EXECUTED (script exists, not run against live cluster)

### LOAD-05: Payment Contention
- VUs: 20 concurrent payment completions
- Target: No duplicate payments, all idempotency keys unique
- Status: NOT EXECUTED (test script not yet created)

### LOAD-06: Sustained Traffic
- VUs: 50 (browsing) + 30 (bookings) sustained over 10 minutes
- Target: p95 < 600ms throughout, HPA scales appropriately
- Status: NOT EXECUTED

---

## 3. Metrics to be Captured (Target)

| Metric | Source | Target |
|---|---|---|
| Request rate (RPS) | k6 http_reqs | > 100 RPS under LOAD-03 |
| p50 response time | k6 http_req_duration | < 200ms |
| p95 response time | k6 http_req_duration | < 600ms |
| p99 response time | k6 http_req_duration | < 1500ms |
| Error rate | k6 http_req_failed | < 2% |
| Booking success rate | custom: successful_bookings | Exactly = available seats |
| Seat conflict rate | custom: seat_conflicts_rejected | Correctly rejected |
| Payment latency | custom: booking_latency_ms p95 | < 800ms |
| Kafka consumer lag | Prometheus kafka_consumer_lag | < 1000 messages |
| Redis operation latency | Prometheus redis_command_duration | < 10ms p95 |
| DB CPU | Prometheus process_cpu_seconds_total | < 80% |
| DB connections | Prometheus hikaricp_connections_active | < 80% of pool |
| JVM heap used | Prometheus jvm_memory_used_bytes | < 85% of limit |
| GC pause time | Prometheus jvm_gc_pause_seconds | < 200ms p95 |
| Pod count (HPA) | kubectl get hpa | Scales from min to max |
| HPA behavior | kubectl describe hpa | No oscillation |

---

## 4. HPA Autoscaling Behavior (Expected)

### Pre-test State
- booking-service: 2 replicas (minReplicas)
- gateway-service: 2 replicas (minReplicas)

### Under LOAD-03 (100 VUs)
- Expected: booking-service HPA triggers (CPU > 70%)
- Expected scale-out: 2 → 4 replicas within 2-3 minutes
- No oscillation (stabilizationWindowSeconds=300 for scale-down)

### Stabilization
- New pods must pass readinessProbe before receiving traffic
- PDB (minAvailable=2) is respected during rolling changes

### Post-test
- Scale-down begins after 5 minutes (300s window)
- Scales back to minReplicas gradually (maxDecrease=20% per 60s)

---

## 5. Load Test Execution Instructions

When a staging cluster is available:

```bash
# Prerequisites
# 1. Deploy staging cluster with multi-replica values
helm upgrade --install vibecheck k8s/helm/vibecheck/ \
  -f k8s/helm/vibecheck/values-prod.yaml \
  --namespace vibecheck --create-namespace

# 2. Seed test data (create show + seats for testing)
# Record SHOW_ID and SEAT_ID from the seeded data

# 3. Run baseline test
k6 run tests/k6/seat-concurrency-stress-test.js \
  -e GATEWAY_URL=http://<staging-gateway>:8079 \
  -e SHOW_ID=<seeded-show-id> \
  -e SEAT_ID=<seeded-seat-id>

# 4. Run with output to InfluxDB / Prometheus remote write for dashboards
k6 run tests/k6/seat-concurrency-stress-test.js \
  --out influxdb=http://influxdb:8086/k6 \
  -e GATEWAY_URL=http://<staging-gateway>:8079

# 5. Monitor HPA during test
kubectl get hpa -n vibecheck -w

# 6. After test: capture metrics
kubectl top pods -n vibecheck
```

---

## 6. Test Execution Results

| Test | Status | p50 | p95 | p99 | Error Rate | Notes |
|---|---|---|---|---|---|---|
| LOAD-01 Baseline | NOT EXECUTED | - | - | - | - | No staging cluster |
| LOAD-02 Moderate | NOT EXECUTED | - | - | - | - | No staging cluster |
| LOAD-03 High Concurrency | NOT EXECUTED | - | - | - | - | No staging cluster |
| LOAD-04 Seat Contention | NOT EXECUTED | - | - | - | - | Script exists |
| LOAD-05 Payment Contention | NOT EXECUTED | - | - | - | - | Script pending |
| LOAD-06 Sustained | NOT EXECUTED | - | - | - | - | No staging cluster |

**All load test results are NOT EXECUTED.**
No results are fabricated or estimated.

---

## 7. Infrastructure Metrics During Load (Target)

| Metric | Pre-Load | Peak Load | Post-Load | Status |
|---|---|---|---|---|
| booking-service replicas | 2 | Expected 4-6 | Expected 2 | NOT EXECUTED |
| gateway-service replicas | 2 | Expected 3-5 | Expected 2 | NOT EXECUTED |
| Kafka consumer lag | 0 | Expected < 500 | Expected 0 | NOT EXECUTED |
| Redis used memory | Baseline | Expected +10% | Baseline | NOT EXECUTED |
| MySQL connections | Baseline | Expected < 50 | Baseline | NOT EXECUTED |

---

## 8. Known Gaps Before Load Testing Can Execute

1. Staging Kubernetes cluster (multi-node, multi-replica) must be provisioned
2. Docker images must be built and pushed to a registry accessible by the cluster
3. Test data seeding script must create shows, theatres, seats for k6 to use
4. PAYMENT_GATEWAY_PROVIDER must be set to MOCK in staging to avoid real charges
5. Kafka must be deployed with 3-broker HA configuration (implemented in M32)
6. metrics-server must be installed for HPA to function
7. Load test results must be exported to a metrics backend (InfluxDB, Prometheus)
