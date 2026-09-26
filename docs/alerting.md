# Operational Alerting Guide & Rule Definitions — VibeCheck Platform

**Document Version:** 1.0  
**Milestone:** 31 — Production Operations & Disaster Recovery  
**Date:** 2026-09-26  
**Format:** Prometheus Alerting Rules (`prometheus.rules.yml`)

---

## 1. Alerting Strategy

The VibeCheck alerting architecture follows Google SRE Four Golden Signals (Latency, Traffic, Errors, Saturation). Alerts are classified into two severity tiers:

- **`critical` (P0/P1):** Immediate pager notification. Direct customer impact or platform outage. Requires SRE intervention within 15 minutes.
- **`warning` (P2/P3):** Ticket creation or Slack channel notification. Potential degradation or backlog accumulation that may escalate if unaddressed.

---

## 2. Prometheus Alert Rules Specification

Below is the complete, validated Prometheus alert rule definition file:

```yaml
groups:
  - name: vibecheck_infrastructure_alerts
    rules:

      # =====================================================================
      # 1. Service Availability
      # =====================================================================
      - alert: ServiceUnavailable
        expr: up{job=~".*-service"} == 0
        for: 1m
        labels:
          severity: critical
          tier: application
        annotations:
          summary: "Microservice {{ $labels.job }} is DOWN"
          description: "Service {{ $labels.job }} in namespace {{ $labels.namespace }} has been unreachable for more than 1 minute."
          runbook_url: "docs/runbooks/service-down.md"

      # =====================================================================
      # 2. HTTP 5xx Error Rate Spike
      # =====================================================================
      - alert: HighHttp5xxRate
        expr: |
          (
            sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) by (service)
            /
            sum(rate(http_server_requests_seconds_count[5m])) by (service)
          ) > 0.05
        for: 2m
        labels:
          severity: critical
          tier: application
        annotations:
          summary: "High HTTP 5xx error rate on {{ $labels.service }}"
          description: "Service {{ $labels.service }} is experiencing > 5% HTTP 5xx errors over a 5-minute rolling window."
          runbook_url: "docs/runbooks/service-down.md"

      # =====================================================================
      # 3. Database Availability & Connection Pool Exhaustion
      # =====================================================================
      - alert: DatabaseUnavailable
        expr: mysql_up == 0
        for: 30s
        labels:
          severity: critical
          tier: datastore
        annotations:
          summary: "MySQL database is DOWN"
          description: "MySQL instance vibecheck-mysql / mysql-0 is not accepting connections."
          runbook_url: "docs/runbooks/mysql-recovery.md"

      - alert: DatabaseConnectionPoolExhausted
        expr: |
          hikaricp_connections_pending > 5
        for: 1m
        labels:
          severity: critical
          tier: datastore
        annotations:
          summary: "HikariCP pool exhausted on {{ $labels.app }}"
          description: "{{ $labels.app }} has > 5 threads blocked waiting for a MySQL connection from HikariPool."
          runbook_url: "docs/runbooks/mysql-recovery.md"

      # =====================================================================
      # 4. Redis Availability
      # =====================================================================
      - alert: RedisUnavailable
        expr: redis_up == 0
        for: 30s
        labels:
          severity: critical
          tier: datastore
        annotations:
          summary: "Redis cache / distributed lock engine is DOWN"
          description: "Redis instance vibecheck-redis / redis-0 is unreachable. Distributed locks and rate limiting impaired."
          runbook_url: "docs/runbooks/redis-recovery.md"

      # =====================================================================
      # 5. Kafka Broker Availability
      # =====================================================================
      - alert: KafkaBrokerUnavailable
        expr: kafka_broker_up == 0
        for: 1m
        labels:
          severity: critical
          tier: messaging
        annotations:
          summary: "Kafka broker is DOWN"
          description: "Kafka broker vibecheck-kafka is unreachable. Outbox relay and notification consumers halted."
          runbook_url: "docs/runbooks/kafka-recovery.md"

      # =====================================================================
      # 6. Kafka Consumer Lag
      # =====================================================================
      - alert: KafkaConsumerLagCritical
        expr: sum(kafka_consumergroup_lag) by (consumergroup, topic) > 500
        for: 3m
        labels:
          severity: warning
          tier: messaging
        annotations:
          summary: "Kafka consumer lag critical for group {{ $labels.consumergroup }}"
          description: "Consumer group {{ $labels.consumergroup }} on topic {{ $labels.topic }} has accumulated > 500 unconsumed records."
          runbook_url: "docs/runbooks/kafka-recovery.md"

      # =====================================================================
      # 7. Transactional Outbox Backlog
      # =====================================================================
      - alert: OutboxPendingBacklogHigh
        expr: vibecheck_outbox_pending_count > 100
        for: 3m
        labels:
          severity: warning
          tier: messaging
        annotations:
          summary: "Transactional Outbox backlog exceeds 100 events"
          description: "Pending events in outbox_events table have remained above 100 for 3 minutes. Indicates Kafka publish congestion."
          runbook_url: "docs/runbooks/outbox-backlog.md"

      # =====================================================================
      # 8. Outbox Dead-Letter / Permanent Failures
      # =====================================================================
      - alert: OutboxDeadLetterGrowth
        expr: increase(vibecheck_outbox_failed_total[10m]) > 5
        for: 1m
        labels:
          severity: critical
          tier: messaging
        annotations:
          summary: "Outbox relay experiencing permanent delivery failures"
          description: "More than 5 outbox events transitioned to DEAD_LETTER / FAILED in the last 10 minutes."
          runbook_url: "docs/runbooks/outbox-backlog.md"

      - alert: DeadLetterTopicEventDetected
        expr: increase(kafka_topic_partition_current_offset{topic=~".*\\.DLT"}[5m]) > 0
        for: 1m
        labels:
          severity: warning
          tier: messaging
        annotations:
          summary: "Events routed to Dead-Letter Topic {{ $labels.topic }}"
          description: "Unprocessable poison pill records routed to DLT in the last 5 minutes."
          runbook_url: "docs/runbooks/dlt-recovery.md"

      # =====================================================================
      # 9. Payment Failure Spike
      # =====================================================================
      - alert: PaymentFailureSpike
        expr: |
          (
            sum(rate(vibecheck_payment_failed_total[5m]))
            /
            sum(rate(vibecheck_payment_initiated_total[5m]))
          ) > 0.15
        for: 2m
        labels:
          severity: critical
          tier: business
        annotations:
          summary: "Payment failure rate exceeds 15%"
          description: "Payment transactions are failing at > 15% rate over 5 minutes. Possible 3rd party gateway outage."
          runbook_url: "docs/runbooks/payment-failure.md"

      # =====================================================================
      # 10. Booking Failure Spike
      # =====================================================================
      - alert: BookingFailureSpike
        expr: |
          (
            sum(rate(vibecheck_booking_created_total{status="FAILED"}[5m]))
            /
            sum(rate(vibecheck_booking_created_total[5m]))
          ) > 0.10
        for: 2m
        labels:
          severity: critical
          tier: business
        annotations:
          summary: "Booking failure rate exceeds 10%"
          description: "Booking creations are failing at > 10% rate over 5 minutes. Investigate seat contention or show service."
          runbook_url: "docs/runbooks/booking-failure.md"

      # =====================================================================
      # 11. Storage & Disk Pressure
      # =====================================================================
      - alert: DiskSpacePressureWarning
        expr: (node_filesystem_avail_bytes{mountpoint=~"/var/lib/mysql|/var/lib/kafka/data|/data"} / node_filesystem_size_bytes) < 0.20
        for: 5m
        labels:
          severity: warning
          tier: infrastructure
        annotations:
          summary: "Storage volume below 20% capacity on {{ $labels.mountpoint }}"
          description: "Persistent storage on {{ $labels.mountpoint }} has less than 20% available free space."
          runbook_url: "docs/runbooks/mysql-recovery.md"

      - alert: DiskSpacePressureCritical
        expr: (node_filesystem_avail_bytes{mountpoint=~"/var/lib/mysql|/var/lib/kafka/data|/data"} / node_filesystem_size_bytes) < 0.10
        for: 1m
        labels:
          severity: critical
          tier: infrastructure
        annotations:
          summary: "Storage volume below 10% capacity on {{ $labels.mountpoint }}"
          description: "Imminent danger of database read-only lockdown or Kafka broker crash due to full disk."
          runbook_url: "docs/runbooks/mysql-recovery.md"
```

---

## 3. Integration with Alertmanager

When Alertmanager is deployed in Kubernetes, Prometheus routes alerts via the following standard configuration block:

```yaml
alerting:
  alertmanagers:
    - static_configs:
        - targets: ['alertmanager:9093']
```

Notification channels configured in Alertmanager:
1. **Critical:** PagerDuty / OpsGenie on-call escalation rotation + P1 incident channel.
2. **Warning:** Slack `#ops-vibecheck-alerts` channel.
