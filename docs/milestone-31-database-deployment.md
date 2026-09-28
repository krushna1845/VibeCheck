# Milestone 31 — Database Deployment Safety & Migration Policy

**Platform:** VibeCheck Movie Booking Platform  
**Auditor:** Principal Backend Engineer / SRE / Platform Engineer  
**Date:** 2026-09-28  
**Scope:** Flyway schema migration lifecycle, multi-replica concurrency safety, and zero-downtime evolution.

---

## 1. Executive Summary

In a distributed microservices environment with multiple container replicas running concurrently, database migrations represent the highest risk vector for catastrophic downtime, schema lockups, and unrecoverable data loss.

This policy defines the database deployment rules, schema migration ordering, concurrency locking mechanics, and zero-downtime schema evolution patterns enforced across all 8 microservices.

---

## 2. Production Core Principles

1. **Strict Prohibition of `ddl-auto: update`:**
   * Every Spring Boot service in VibeCheck configures:
     ```yaml
     spring:
       jpa:
         hibernate:
           ddl-auto: validate
     ```
   * Hibernate is strictly forbidden from altering, creating, or dropping database objects at runtime. Hibernate validates entity mappings against the database catalog on startup. If an entity field does not match the database column, the application context refuses to boot rather than silently generating invalid schema changes.
2. **Flyway as Single Source of Truth:**
   * All schema changes are versioned SQL scripts in `src/main/resources/db/migration/V{N}__{description}.sql`.
   * Migrations are strictly immutable once applied. Checksums are recorded in `flyway_schema_history` and validated on every startup.
3. **InitContainers Ordering:**
   * Helm service templates execute initContainers (`waitForMySQL`) ensuring MySQL port 3306 is responsive and accepting connections before the Spring application process begins executing migrations.

---

## 3. Multi-Replica Concurrent Startup & Advisory Locking

When a microservice scales to 3 or 4 replicas in production, multiple pods boot simultaneously. 

### 3.1 Flyway Table Locking
* Flyway automatically acquires an exclusive lock on the `flyway_schema_history` metadata table before executing any migration script.
* **Mechanism:** 
  - In MySQL 8.0, Flyway executes a table lock or transaction lock on `flyway_schema_history`.
  - **Replica 1:** Acquires lock, detects pending migrations, executes DDL, updates history, commits, and releases lock.
  - **Replicas 2, 3, 4:** Wait for lock acquisition. Upon acquiring lock, they query `flyway_schema_history`, detect all migrations are already applied (`Current version: N, latest version: N`), and proceed directly to Spring application context initialization without re-executing scripts.
* **Concurrency Verification:** Multiple replicas booting concurrently will never execute the same migration twice or collide on table creation.

---

## 4. Zero-Downtime Migration Pattern: Expand / Contract

To maintain 100% uptime during rolling updates, database migrations must adhere to the **Expand and Contract (Parallel Run)** design pattern:

```text
Phase 1 (Old App):    Writes to old_column.
Phase 2 (Migration):  Adds new_column as NULLABLE or with default. (Expand)
Phase 3 (Rolling):    New App writes to BOTH old_column and new_column, reads from new_column.
Phase 4 (Migration):  Backfill old data into new_column.
Phase 5 (Next App):   New App reads and writes exclusively to new_column.
Phase 6 (Contract):   Drop old_column in a later release. (Contract)
```

### 4.1 Strict Migration Rules
1. **Never Rename Columns in a Single Step:** Renaming a column immediately breaks running old-version pods before the rolling update finishes.
2. **Never Add NOT NULL Columns Without Defaults:** Adding a `NOT NULL` column without a default value will cause writes from old application replicas to fail.
3. **Never Drop Columns In Use:** Columns must only be dropped in a subsequent release after all deployed code has ceased referencing them.
4. **Index Creation with Care:** On large production tables, create indexes using non-blocking syntax where supported.

---

## 5. Failed Migration Behavior & Recovery

If a migration fails mid-execution (e.g. syntax error or disk exhaustion):

1. **State in `flyway_schema_history`:** The failed migration is marked with `success = 0`.
2. **Startup Block:** Spring Boot will refuse to start any subsequent replicas, logging `FlywayException: Migration V{N} failed`.
3. **Safe Recovery Procedure:**
   ```bash
   # 1. Connect to MySQL target database
   mysql -h mysql -u root -p"$DB_ROOT_PASSWORD" vibecheck_booking

   # 2. Inspect the failed script
   SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;

   # 3. Manually undo the partial DDL statements executed by the failed script
   # (e.g. DROP TABLE or ALTER TABLE rollback)

   # 4. Remove the failed migration record from history
   DELETE FROM flyway_schema_history WHERE version = 'X' AND success = 0;

   # 5. Fix the migration SQL script in code and redeploy
   ```

---

## 6. Rollback Strategy

* **Destructive Database Rollback Is Prohibited:** Never run automated reverse DDL (`V{N}__rollback.sql` or dropping tables) during an incident. Dropping tables destroys customer data.
* **Code Rollback First:** If a new release has application bugs, roll back the Kubernetes deployment to the previous image tag (`kubectl rollout undo`). Because the database was migrated using the expand/contract pattern (backward-compatible additions), the previous application image will continue functioning normally with the updated database schema.
