# Milestone 32 — Database High Availability Strategy

**Date:** 2026-09-28
**Status:** DOCUMENTED (architecture design); SIMULATED (local testing); NOT EXECUTED (managed cloud DB unavailable)

---

## 1. Environment Separation

| Environment | Database | Topology | Notes |
|---|---|---|---|
| Local development | Docker Compose MySQL 8.0 | Single instance | Developer convenience |
| CI | In-memory / Testcontainers | Ephemeral | Unit tests use mocks |
| Minikube | Kubernetes StatefulSet MySQL | Single instance | Local K8s testing |
| Staging | Managed MySQL (target) | Multi-AZ primary/replica | Pre-production validation |
| Production | Managed MySQL (target) | Multi-AZ, automated failover | Full HA |

---

## 2. Production Database Architecture (Target)

The Milestone 31 audit explicitly identified that a single Kubernetes StatefulSet MySQL
is NOT acceptable for production. The following defines the target managed architecture.

### 2.1 No Cloud Provider Commitment Found

Inspection of the repository found no existing cloud provider commitment:
- No AWS IAM annotations, no AWS credentials configuration
- No GCP service account configuration
- No Azure managed identity configuration

This document defines the architecture in cloud-neutral terms and provides
provider-specific guidance for the most common options.

### 2.2 Target Architecture

```
Application Services (K8s Pods)
        |
        | TLS encrypted connection
        v
Load Balancer / Proxy Endpoint (cloud-managed, failover-transparent)
        |
        +---> Primary DB Instance (AZ-1)
        |         Synchronous replication
        +---> Standby Replica (AZ-2)  <-- automated failover target
        |
        +---> Read Replica(s) (optional, read-heavy services)
```

### 2.3 Required Database Properties

| Property | Requirement | Rationale |
|---|---|---|
| Engine | MySQL 8.0 | Application compatibility |
| Multi-AZ | REQUIRED | Failover without manual intervention |
| Automated backups | REQUIRED | PITR compliance |
| Encryption at rest | REQUIRED | Security baseline |
| Encryption in transit | REQUIRED | TLS to all connections |
| Automated failover | REQUIRED | RTO target |
| Monitoring | REQUIRED | CloudWatch / Cloud Monitoring |
| Controlled credentials | REQUIRED | No plaintext in Git |
| Connection pooling | RECOMMENDED | Connection limit management |

---

## 3. Provider-Specific Guidance

### 3.1 AWS RDS / Aurora MySQL

```yaml
# Conceptual Terraform (NOT deployed -- no AWS credentials in repo)
resource "aws_db_instance" "vibecheck_primary" {
  engine                  = "mysql"
  engine_version          = "8.0"
  instance_class          = "db.t3.medium"
  allocated_storage       = 100
  multi_az                = true
  storage_encrypted       = true
  deletion_protection     = true
  backup_retention_period = 7          # Days for PITR
  backup_window           = "03:00-04:00"
  maintenance_window      = "Mon:04:00-Mon:05:00"
  skip_final_snapshot     = false
  final_snapshot_identifier = "vibecheck-final-snapshot"
  
  # TLS enforcement
  parameter_group_name = aws_db_parameter_group.vibecheck.name  # require_secure_transport=ON
}
```

**Aurora MySQL** is preferred over RDS MySQL for production:
- Up to 15 read replicas (vs 5 for RDS)
- Faster failover (typically 30 seconds vs 60-120 seconds for RDS)
- Serverless v2 option for variable workloads
- Storage automatically grows in 10GB increments

### 3.2 Google Cloud SQL for MySQL

```yaml
# Conceptual Cloud SQL config (NOT deployed)
availability_type: REGIONAL   # Multi-zone HA
backup_configuration:
  enabled: true
  start_time: "03:00"
  binary_log_enabled: true    # Required for PITR
  backup_retention_settings:
    retained_backups: 7
settings:
  tier: db-n1-standard-2
  disk_size: 100
  disk_autoresize: true
  ip_configuration:
    require_ssl: true
    ipv4_enabled: false        # No public IP
    private_network: vpc_link
```

### 3.3 Azure Database for MySQL Flexible Server

- Flexible Server with Zone Redundant HA
- Binary log retention for PITR
- SSL enforcement via server parameter

---

## 4. Recovery Objectives

| Metric | Target | Basis |
|---|---|---|
| RPO (Recovery Point Objective) | < 5 minutes | Automated backup + binary log replay |
| RTO (Recovery Time Objective) | < 60 seconds | Managed automated failover (Aurora) |
| RTO (Manual restore from backup) | < 30 minutes | Based on backup restore testing |
| Backup Retention | 7 days minimum | Covers weekly booking cycles |
| PITR Range | Last 7 days | Automated backup window |

**IMPORTANT:** These are TARGET objectives based on managed service SLAs.
They have NOT been experimentally verified. Mark status: TARGET (unverified).

---

## 5. Backup Policy

### 5.1 Automated Backup Schedule
- Full backup: Daily at 03:00 UTC (off-peak for Indian timezone)
- Binary log shipping: Continuous (every 5 minutes to backup storage)
- Retention: 7 days of automated backups + 1 final snapshot on deletion
- Cross-region copy: Enabled for production (same-region for staging)

### 5.2 Manual Backup (Current Capability - Docker Compose)
Existing scripts from Milestone 31:
- scripts/backup/mysql-backup.ps1 -- mysqldump with gzip
- scripts/backup/mysql-restore.ps1 -- restore from .sql.gz
- scripts/backup/verify-backup.ps1 -- integrity verification

These remain valid for:
- Local development backup
- Pre-migration snapshots
- CI/CD database seeding

### 5.3 Backup Integrity Verification
Restore procedure is tested in: scripts/milestone-32-database-recovery-test.ps1
Verification checks:
- Flyway schema history intact
- All 7 databases restored
- Key tables (bookings, outbox_events, processed_events) present
- Row count integrity

---

## 6. Point-in-Time Recovery (PITR)

### 6.1 Current State
PITR is NOT configured for the Kubernetes StatefulSet MySQL. Binary logging
is not enabled in the mysql-statefulset.yaml.

To enable PITR on the local StatefulSet (staging only), add to MySQL config:
```
[mysqld]
log_bin = mysql-bin
binlog_format = ROW
expire_logs_days = 7
max_binlog_size = 100M
```

### 6.2 Production PITR
Managed database services provide automated PITR:
- AWS RDS/Aurora: Restore to any second within retention window
- Cloud SQL: Restore to any point within binary log retention
- Azure: Point-in-time restore via portal/CLI

### 6.3 PITR Procedure (Target)
```
1. Identify recovery timestamp (T_target)
2. Restore latest automated backup before T_target
3. Apply binary logs from backup restore point to T_target
4. Validate Flyway schema_history matches expected version
5. Validate booking/payment state consistency
6. Redirect application connection string to restored instance
7. Restart services to reconnect
```

---

## 7. Flyway Compatibility

### 7.1 Migration Strategy
Flyway migrations run on application startup via Spring Boot autoconfiguration.
On restore:
1. Restored DB contains the flyway_schema_history table
2. On application restart, Flyway reads the history table
3. If restored DB is at the same migration version as the application, no migrations run
4. If restored DB is behind (e.g., from a backup before a migration), Flyway applies missing migrations

### 7.2 Risks
- If a migration was partially applied before the failure, Flyway may report a failed migration
- Resolution: manually mark the failed migration as resolved, or restore to a pre-migration backup
- Never delete flyway_schema_history entries manually without full understanding

### 7.3 Validation After Restore
```sql
-- Check migration state
SELECT version, description, success, installed_on
FROM flyway_schema_history
ORDER BY installed_rank;

-- Verify all migrations succeeded
SELECT COUNT(*) FROM flyway_schema_history WHERE success = 0;  -- Should be 0
```

---

## 8. Connection Configuration

### 8.1 Current (Insecure - Local Only)
```
jdbc:mysql://mysql:3306/vibecheck_booking?useSSL=false&allowPublicKeyRetrieval=true
```

### 8.2 Staging/Production (Secure)
```
jdbc:mysql://<managed-endpoint>:3306/vibecheck_booking
  ?useSSL=true
  &requireSSL=true
  &verifyServerCertificate=true
  &sslMode=VERIFY_CA
  &serverTimezone=UTC
  &characterEncoding=UTF-8
```

### 8.3 Kubernetes Configuration
Production connection via External Secret + ConfigMap:
```yaml
# In services deployment env:
- name: SPRING_DATASOURCE_URL
  valueFrom:
    configMapKeyRef:
      name: vibecheck-db-config
      key: DATASOURCE_URL
- name: SPRING_DATASOURCE_PASSWORD
  valueFrom:
    secretKeyRef:
      name: vibecheck-db-secret  # From ExternalSecret
      key: db-password
```

---

## 9. Migration Strategy (Single StatefulSet to Managed DB)

### Step 1: Pre-migration
1. Take a full mysqldump of all vibecheck databases
2. Verify backup integrity with verify-backup.ps1
3. Create managed DB instance with same MySQL version (8.0)
4. Configure TLS, encryption, and network access

### Step 2: Schema Migration
1. Restore dump to managed DB
2. Verify all Flyway migrations are present in flyway_schema_history
3. Run Flyway baseline if needed (managed DB with empty schema)

### Step 3: Cutover
1. Stop all application services (maintenance window)
2. Take final incremental backup
3. Restore incremental to managed DB
4. Update Kubernetes ConfigMap with new DB endpoint
5. Update ExternalSecret with new credentials
6. Start application services
7. Verify health endpoints
8. Verify booking/payment/outbox consistency

### Step 4: Validation
1. Test end-to-end booking flow
2. Verify Flyway state
3. Monitor connection pool metrics
4. Verify Prometheus DB connection metrics

---

## 10. Security Requirements

| Requirement | Local StatefulSet | Production Managed |
|---|---|---|
| Encryption at rest | NO | REQUIRED |
| TLS in transit | NO (useSSL=false) | REQUIRED |
| Private network only | YES (ClusterIP) | REQUIRED (no public IP) |
| Root user access | YES (root password only) | NO (dedicated service user) |
| Credential rotation | MANUAL | AUTOMATED (External Secrets) |
| Audit logging | NO | REQUIRED |
| Network restriction | NetworkPolicy | VPC/Security Group |
