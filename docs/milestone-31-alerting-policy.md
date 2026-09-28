# Milestone 31 — Production Alerting Policy & SLI / SLO Specifications

**Platform:** VibeCheck Movie Booking Platform  
**Auditor:** Principal Backend Engineer / SRE / Platform Engineer  
**Date:** 2026-09-28  
**Scope:** Service Level Indicators (SLIs), Service Level Objectives (SLOs), Prometheus alert rules, and operational escalation matrices.

---

## 1. Executive Summary

This alerting policy formalizes the operational monitoring rules for the VibeCheck platform. Alert rules are engineered to prioritize actionable anomalies over noisy false positives, aligning with the Google SRE Four Golden Signals (Latency, Traffic, Errors, Saturation).

Every alert defines:
- A specific PromQL metric expression.
- Evaluation window and duration threshold.
- Explicit severity tier (P0 = Critical Outage, P1 = Business Degradation, P2 = Warning / Capacity).
- Documented engineering assumptions justifying why the threshold was selected.
- Clickable link to the corresponding operational runbook.

---

## 2. SLI / SLO Target Definitions

| Service Level Objective (SLO) | Target | Measurement Metric (SLI) | Compliance Window |
| :--- | :--- | :--- | :--- |
| **API Availability** | **99.9%** | Successful HTTP responses (`status < 500`) / Total requests | Rolling 30 days |
| **API Latency (p95)** | **< 300 ms** | `http_server_requests_seconds{quantile="0.95"}` (excluding reports) | Rolling 7 days |
| **Booking Success Rate** | **> 98.0%** | `vibecheck_booking_created_total{status="CONFIRMED"}` / Total attempts | Rolling 24 hours |
| **Payment Webhook Processing**| **< 2.0 s** | Time from webhook arrival to booking confirmed event publish | Rolling 24 hours |
| **Outbox Relay Latency** | **< 5.0 s** | Time from outbox row insert to Kafka broker acknowledgment | Rolling 24 hours |

---

## 3. Production Alert Rules Specification

### 3.1 Tier 1: Service Availability & Error Rates (P1 Critical)

#### Alert: `ServiceUnavailable`
* **PromQL:** `up{job=~".*-service"} == 0`
* **For:** `1m`
* **Severity:** `critical` (P1)
* **Assumption:** Any microservice pod remaining unreachable for 60 seconds indicates container crashloop, pod scheduling failure, or complete network partition.
* **Runbook:** [`docs/runbooks/service-down.md`](file:///c:/Users/acer/Downloads/booking-system/docs/runbooks/service-down.md)

#### Alert: `HighHttp5xxRate`
* **PromQL:**
  ```promql
  (
    sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (service)
    /
    sum(rate(http_server_requests_seconds_count[5m])) by (service)
  ) > 0.05
  ```
* **For:** `2m`
* **Severity:** `critical` (P1)
* **Assumption:** Error rate exceeding 5% for 2 consecutive minutes indicates a systemic application bug, database deadlock, or downstream service failure. Normal transient error rate is < 0.1%.
* **Runbook:** [`docs/runbooks/service-down.md`](file:///c:/Users/acer/Downloads/booking-system/docs/runbooks/service-down.md)

#### Alert: `HighP99Latency`
* **PromQL:** `histogram_quantile(0.99, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, service)) > 1.0`
* **For:** `3m`
* **Severity:** `warning` (P2)
* **Assumption:** p99 latency exceeding 1000ms degrades end-user checkout experience and can cascade into gateway circuit breaker trips.
* **Runbook:** [`docs/runbooks/service-down.md`](file:///c:/Users/acer/Downloads/booking-system/docs/runbooks/service-down.md)

---

### 3.2 Tier 2: Stateful Infrastructure & Connection Saturation (P0/P1)

#### Alert: `DatabaseUnavailable`
* **PromQL:** `mysql_up == 0`
* **For:** `30s`
* **Severity:** `critical` (P0)
* **Assumption:** MySQL is the authoritative relational store for all 8 services. Loss of MySQL immediately paralyzes all transactional operations.
* **Runbook:** [`docs/runbooks/mysql-recovery.md`](file:///c:/Users/acer/Downloads/booking-system/docs/runbooks/mysql-recovery.md)

#### Alert: `DatabaseConnectionPoolExhausted`
* **PromQL:** `hikaricp_connections_pending{pool="HikariPool-1"} > 5`
* **For:** `1m`
* **Severity:** `critical` (P1)
* **Assumption:** More than 5 threads queued waiting for a free database connection for 60 seconds indicates slow unindexed queries, database lock contention, or undersized pool capacity.
* **Runbook:** [`docs/runbooks/mysql-recovery.md`](file:///c:/Users/acer/Downloads/booking-system/docs/runbooks/mysql-recovery.md)

#### Alert: `RedisUnavailable`
* **PromQL:** `redis_up == 0`
* **For:** `30s`
* **Severity:** `critical` (P1)
* **Assumption:** Redis provides distributed seat locking and gateway rate-limiting. Outage impairs new seat reservations.
* **Runbook:** [`docs/runbooks/redis-recovery.md`](file:///c:/Users/acer/Downloads/booking-system/docs/runbooks/redis-recovery.md)

#### Alert: `KafkaBrokerUnavailable`
* **PromQL:** `kafka_broker_up == 0`
* **For:** `1m`
* **Severity:** `critical` (P1)
* **Assumption:** Kafka broker downtime halts asynchronous event publishing and notification delivery.
* **Runbook:** [`docs/runbooks/kafka-recovery.md`](file:///c:/Users/acer/Downloads/booking-system/docs/runbooks/kafka-recovery.md)

---

### 3.3 Tier 3: Asynchronous Messaging & Outbox Integrity (P1/P2)

#### Alert: `KafkaConsumerLagCritical`
* **PromQL:** `sum(kafka_consumergroup_lag) by (consumergroup, topic) > 500`
* **For:** `3m`
* **Severity:** `warning` (P2)
* **Assumption:** Consumer lag exceeding 500 messages indicates notification workers are saturated, slow email/SMS gateways, or consumer partition starvation.
* **Runbook:** [`docs/runbooks/kafka-recovery.md`](file:///c:/Users/acer/Downloads/booking-system/docs/runbooks/kafka-recovery.md)

#### Alert: `OutboxPendingBacklogHigh`
* **PromQL:** `vibecheck_outbox_pending_count > 100`
* **For:** `3m`
* **Severity:** `warning` (P2)
* **Assumption:** Transactional outbox table accumulating > 100 events indicates the relay scheduler is blocked, experiencing Kafka timeouts, or database transaction contention.
* **Runbook:** [`docs/runbooks/outbox-backlog.md`](file:///c:/Users/acer/Downloads/booking-system/docs/runbooks/outbox-backlog.md)

#### Alert: `OutboxDeadLetterGrowth`
* **PromQL:** `increase(vibecheck_outbox_failed_total[10m]) > 5`
* **For:** `1m`
* **Severity:** `critical` (P1)
* **Assumption:** Events transitioning to permanent failure status indicates serialization bugs or repeated unrecoverable network drops.
* **Runbook:** [`docs/runbooks/outbox-backlog.md`](file:///c:/Users/acer/Downloads/booking-system/docs/runbooks/outbox-backlog.md)

#### Alert: `DeadLetterTopicEventDetected`
* **PromQL:** `increase(kafka_topic_partition_current_offset{topic=~".*\\.DLT"}[5m]) > 0`
* **For:** `1m`
* **Severity:** `warning` (P2)
* **Assumption:** Any poison pill routed to `.DLT` represents an unfulfilled notification or ticket update requiring manual engineer triage.
* **Runbook:** [`docs/runbooks/dlt-recovery.md`](file:///c:/Users/acer/Downloads/booking-system/docs/runbooks/dlt-recovery.md)

---

### 3.4 Tier 4: Business Funnel Metrics (P0/P1)

#### Alert: `PaymentFailureSpike`
* **PromQL:**
  ```promql
  (
    sum(rate(vibecheck_payment_failed_total[5m]))
    /
    sum(rate(vibecheck_payment_initiated_total[5m]))
  ) > 0.15
  ```
* **For:** `2m`
* **Severity:** `critical` (P0)
* **Assumption:** Payment failure rate exceeding 15% directly damages business revenue. Typically indicates external payment processor (Razorpay/Stripe) outage or expired API keys.
* **Runbook:** [`docs/runbooks/payment-failure.md`](file:///c:/Users/acer/Downloads/booking-system/docs/runbooks/payment-failure.md)

#### Alert: `BookingFailureSpike`
* **PromQL:**
  ```promql
  (
    sum(rate(vibecheck_booking_created_total{status="FAILED"}[5m]))
    /
    sum(rate(vibecheck_booking_created_total[5m]))
  ) > 0.10
  ```
* **For:** `2m`
* **Severity:** `critical` (P1)
* **Assumption:** Booking creations failing at > 10% indicates seat lock deadlock, show expiration desynchronization, or inventory database lockups.
* **Runbook:** [`docs/runbooks/booking-failure.md`](file:///c:/Users/acer/Downloads/booking-system/docs/runbooks/booking-failure.md)
