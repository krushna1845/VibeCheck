# Operational Runbook: MySQL Recovery & Troubleshooting

**Runbook ID:** RB-OPS-002  
**Target:** MySQL 8.0 (`vibecheck-mysql` / `mysql-0`)  
**Severity:** P0 (Platform-Wide Critical Outage)

---

## 1. Symptoms
- Prometheus alert: `MySQLDown` or `DatabaseConnectionExhaustion` firing.
- All microservices log `CommunicationsException: Communications link failure` or `HikariPool - Connection is not available, request timed out after 30000ms`.
- Actuator health check on all microservices reports `status: DOWN` with `details.db.status: DOWN`.
- Users receive HTTP 500 / 503 across all write and read endpoints.

---

## 2. Initial Checks
1. Check whether the MySQL process is running or crashed.
2. Check disk space on the host / PersistentVolume (`df -h`).
3. Check memory pressure or OOM killer activity on the host node.
4. Check whether MySQL InnoDB crash recovery is running.

---

## 3. Commands

### Local (Docker Compose):
```bash
# Check container status and exit code
docker compose ps mysql

# Ping MySQL directly via mysqladmin
docker compose exec mysql mysqladmin -uroot -p"$DB_ROOT_PASSWORD" ping

# Inspect container healthcheck history
docker inspect vibecheck-mysql --format='{{json .State.Health.Log}}' | jq .
```

### Kubernetes:
```bash
# Check StatefulSet and pod status
kubectl get statefulset mysql -n vibecheck
kubectl get pod mysql-0 -n vibecheck

# Execute mysqladmin ping inside pod
kubectl exec -it mysql-0 -n vibecheck -- mysqladmin -uroot -p"$DB_ROOT_PASSWORD" ping

# Check PVC disk capacity
kubectl get pvc mysql-data-mysql-0 -n vibecheck
```

---

## 4. Logs to Inspect

### Local (Docker Compose):
```bash
# View last 300 lines of MySQL logs
docker compose logs --tail=300 vibecheck-mysql

# Check for InnoDB error messages
docker compose logs vibecheck-mysql | grep -iE "InnoDB: Error|mysqld: ready for connections|Disk is full|corrupted"
```

### Kubernetes:
```bash
# View pod logs
kubectl logs mysql-0 -n vibecheck --tail=300

# View previous logs if container restarted
kubectl logs mysql-0 -n vibecheck --previous --tail=100
```

---

## 5. Metrics to Inspect
- `mysql_up` (1 = UP, 0 = DOWN)
- `mysql_global_status_threads_connected` vs `mysql_global_variables_max_connections`
- `mysql_global_status_slow_queries`
- `container_memory_usage_bytes{name="vibecheck-mysql"}`
- `node_filesystem_avail_bytes{mountpoint="/var/lib/mysql"}`

---

## 6. Safe Recovery Procedure

### Scenario A: Unclean Shutdown / Normal Crash
MySQL 8.0 will automatically initiate InnoDB crash recovery on startup.
1. Start / restart container:
   ```bash
   # Docker Compose
   docker compose restart mysql

   # Kubernetes
   kubectl rollout restart statefulset/mysql -n vibecheck
   ```
2. Wait 30–60 seconds for InnoDB redo logs to replay:
   ```bash
   docker compose logs -f mysql | grep -m 1 "ready for connections"
   ```

### Scenario B: Disk Space Exhaustion
If the log indicates `Disk is full` or `No space left on device`:
1. Clean up old backups from `backups/mysql/`:
   ```bash
   pwsh ./scripts/backup/mysql-backup.ps1 -Action cleanup -RetentionDays 3
   ```
2. In Kubernetes, expand PVC storage quota if supported by the StorageClass:
   ```bash
   kubectl patch pvc mysql-data-mysql-0 -n vibecheck -p '{"spec":{"resources":{"requests":{"storage":"10Gi"}}}}'
   ```
3. Restart MySQL.

### Scenario C: Corrupted Database (Point-in-Time Restore)
If InnoDB tables are irreparably corrupted and fail to open:
1. Verify latest valid backup:
   ```bash
   pwsh ./scripts/backup/verify-backup.ps1 -BackupPath (Get-ChildItem backups\mysql\*.sql.gz | Sort-Object LastWriteTime -Descending | Select-Object -First 1).FullName
   ```
2. Restore backup using the safe restore script:
   ```bash
   pwsh ./scripts/backup/mysql-restore.ps1 -BackupFile (Get-ChildItem backups\mysql\*.sql.gz | Sort-Object LastWriteTime -Descending | Select-Object -First 1).FullName -Confirm
   ```
3. Restart microservices to re-establish connection pools:
   ```bash
   docker compose restart booking-service payment-service show-service theatre-service movie-service auth-service notification-service
   ```

---

## 7. Verification
1. Verify MySQL ping responds with `mysqld is alive`:
   ```bash
   docker compose exec mysql mysqladmin -uroot -p"$DB_ROOT_PASSWORD" ping
   ```
2. Verify all 8 VibeCheck schema tables exist:
   ```bash
   docker compose exec mysql mysql -uroot -p"$DB_ROOT_PASSWORD" -e "
     SELECT table_schema, count(*) FROM information_schema.tables 
     WHERE table_schema LIKE 'vibecheck%' GROUP BY table_schema;"
   ```
3. Verify application Actuator health:
   ```bash
   curl -s http://localhost:8084/actuator/health | jq .components.db
   ```

---

## 8. Rollback Procedure
If a database restore failed or restored corrupt data:
- Re-run restore specifying an earlier confirmed-good backup timestamp file from `backups/mysql/`.

---

## 9. Escalation Conditions
- InnoDB reports checksum failure or corruption that prevents mysqld from starting even in `innodb_force_recovery=1` mode.
- Backup verification fails across all available backup archives.
- Total outage duration exceeds 15 minutes.
