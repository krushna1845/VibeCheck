# Milestone 32 — Kafka High Availability Architecture

**Date:** 2026-09-28
**Status:** IMPLEMENTED (Kubernetes/Helm); NOT EXECUTED (live cluster unavailable locally)

---

## 1. Architecture Decision

### Current State (Pre-M32)
- Single Kafka broker (BROKER_ID=1 hardcoded)
- ZooKeeper as ephemeral Deployment (no persistent identity, no PVC)
- KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1
- No min.insync.replicas configured
- Single advertised listener: PLAINTEXT://kafka:9092

### Decision: Retain ZooKeeper Mode

The existing deployment uses Confluent Platform 7.5.0 (Kafka 3.5.x) in ZooKeeper mode.
Migration to KRaft would require:
- Kafka 3.3+ (already satisfied)
- Controller quorum configuration
- Metadata log migration (kafka-storage.sh format)
- Risk to existing consumer group offsets

**Decision:** Retain ZooKeeper mode. Upgrade is not required for production HA. KRaft migration is a future milestone item.

---

## 2. Target Production Topology

```
ZooKeeper (StatefulSet, 1 replica, stable identity: zookeeper-0)
    Stable DNS: zookeeper.vibecheck.svc.cluster.local:2181
         |
         +--- Kafka Broker 0 (kafka-0.kafka-headless:9092)
         |         BROKER_ID=1, leader for some partitions
         +--- Kafka Broker 1 (kafka-1.kafka-headless:9092)
         |         BROKER_ID=2, follower/leader for partitions
         +--- Kafka Broker 2 (kafka-2.kafka-headless:9092)
                   BROKER_ID=3, follower/leader for partitions

Client bootstrap: kafka:9092 (ClusterIP, load-balances to all 3 brokers)
```

---

## 3. HA Configuration Parameters

### Kafka Broker Settings

| Parameter | Value | Purpose |
|---|---|---|
| replicaCount | 3 | Three broker instances |
| KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR | 3 | Consumer group offsets durable across all brokers |
| KAFKA_OFFSETS_TOPIC_NUM_PARTITIONS | 3 | Parallelism for consumer group coordination |
| KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR | 3 | Transactional producer state durable |
| KAFKA_TRANSACTION_STATE_LOG_MIN_ISR | 2 | Min 2 replicas must acknowledge transactions |
| KAFKA_DEFAULT_REPLICATION_FACTOR | 3 | New topics default to 3 replicas |
| KAFKA_MIN_INSYNC_REPLICAS | 2 | Producer acks=all requires 2 live replicas |
| KAFKA_UNCLEAN_LEADER_ELECTION_ENABLE | false | Prevents data loss from stale leader |
| KAFKA_LOG_RETENTION_HOURS | 168 (7 days) | Event recovery window |

### Producer Requirements
For full durability guarantees, application producers must be configured with:
```yaml
spring:
  kafka:
    producer:
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
        max.in.flight.requests.per.connection: 5
```

The outbox relay in booking-service uses Spring Kafka's KafkaTemplate. The existing acks
configuration should be verified against application.yml for each producer service.

---

## 4. Broker Identity and DNS

### StatefulSet Pod Identity
- kafka-0 -> BROKER_ID=1, DNS: kafka-0.kafka-headless.vibecheck.svc.cluster.local
- kafka-1 -> BROKER_ID=2, DNS: kafka-1.kafka-headless.vibecheck.svc.cluster.local
- kafka-2 -> BROKER_ID=3, DNS: kafka-2.kafka-headless.vibecheck.svc.cluster.local

### Dynamic BROKER_ID
The StatefulSet derives BROKER_ID from the pod ordinal index:
```bash
export KAFKA_BROKER_ID=$((${HOSTNAME##*-} + 1))
```
This eliminates the hardcoded BROKER_ID=1 that prevented multi-broker operation.

### Advertised Listeners Per Broker
```bash
KAFKA_ADVERTISED_LISTENERS="PLAINTEXT://${HOSTNAME}.kafka-headless.vibecheck.svc.cluster.local:9092,PLAINTEXT_CLIENT://kafka:9092"
```
This allows:
- Inter-broker replication via stable headless DNS
- Application client connections via the ClusterIP service

---

## 5. ZooKeeper HA Changes

### Previous State
- Kind: Deployment (not StatefulSet)
- No persistent volumes -- ZooKeeper data lost on pod restart
- Kafka brokers needed to re-register with ZooKeeper after restart

### New State
- Kind: StatefulSet (serviceName: zookeeper)
- Two PVCs per pod: zookeeper-data and zookeeper-log
- Stable identity: zookeeper-0.zookeeper.vibecheck.svc.cluster.local
- ZOOKEEPER_SYNC_LIMIT: 2 (follower sync timeout)

---

## 6. Service Architecture

### kafka-headless (ClusterIP: None)
- Purpose: Stable per-broker DNS for inter-broker replication
- publishNotReadyAddresses: true (brokers visible during startup)
- Port: 9092 (PLAINTEXT internal)

### kafka (ClusterIP)
- Purpose: Client application bootstrap
- Port: 9092
- Applications use kafka:9092 as bootstrap server
- Load-balanced across all ready brokers

---

## 7. Environment Configuration Separation

| Environment | Kafka Brokers | Replication Factor | Min ISR |
|---|---|---|---|
| Local (Docker Compose) | 1 | 1 | 1 |
| Minikube | 1 (override) | 1 (override) | 1 (override) |
| Default (values.yaml) | 3 | 3 | 2 |
| Production (values-prod.yaml) | 3 | 3 | 2 |

Docker Compose retains the single-broker configuration for developer experience.
Production/staging Helm deployments use the 3-broker HA topology.

---

## 8. Consumer Group Resilience

With 3 brokers and replication.factor=3:
- Consumer group coordinator is elected from available brokers
- If one broker fails, the coordinator role moves to a surviving broker
- Consumer group rebalance completes typically within 30-60 seconds
- Committed offsets are replicated to all 3 brokers (offsets topic RF=3)
- No committed offset data is lost during single broker failure

### Outbox Relay Recovery
The booking-service OutboxRelayScheduler publishes events from the outbox table.
If a Kafka broker fails mid-publish:
1. The producer retries (retries=3 minimum)
2. Outbox records remain in PENDING/PROCESSING state
3. On next scheduler tick, unpublished records are re-attempted
4. Idempotency is maintained via processed_events table on the consumer side

---

## 9. Verification Checklist

| Check | Method | Status |
|---|---|---|
| 3 Kafka pods start successfully | kubectl get pods -l app.kubernetes.io/name=kafka | NOT EXECUTED |
| All brokers register with ZooKeeper | kafka-broker-api-versions against each broker | NOT EXECUTED |
| Topic created with RF=3 | kafka-topics.sh --describe | NOT EXECUTED |
| ISR=3 for all partitions | kafka-topics.sh --describe | NOT EXECUTED |
| Producer publishes with acks=all | Produce and check response | NOT EXECUTED |
| Outbox relay reaches all brokers | Observe outbox PUBLISHED state | NOT EXECUTED |
| Consumer group healthy across 3 brokers | kafka-consumer-groups.sh | NOT EXECUTED |

Verification requires a running Kubernetes cluster with adequate node capacity.

---

## 10. Known Limitations

1. ZooKeeper remains single-instance -- not a ZooKeeper ensemble. A 3-node ZooKeeper ensemble requires odd quorum (3 or 5 nodes) and significantly more resources. For the VibeCheck platform scale, single ZooKeeper with persistent storage is an acceptable staging trade-off. Production should evaluate a managed Kafka service (Confluent Cloud, AWS MSK).

2. No TLS/SASL configured. All communication is PLAINTEXT within the cluster. TLS configuration requires certificate infrastructure and is documented as a future requirement.

3. Docker Compose retains single-broker. Developer local environment is not impacted by the K8s HA changes.
