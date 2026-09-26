# Operational Runbook: Microservice Down / Unhealthy

**Runbook ID:** RB-OPS-001  
**Target:** VibeCheck Microservices (Gateway, Auth, Movie, Theatre, Show, Booking, Payment, Notification)  
**Severity:** P1 (Gateway/Booking/Payment/Auth) / P2 (Show/Theatre/Movie/Notification)

---

## 1. Symptoms
- Prometheus alert: `ServiceDown` or `HighHttp5xxRate` firing.
- Ingress returning HTTP 502 Bad Gateway or 503 Service Unavailable.
- Actuator health check failing on `/actuator/health` or `/actuator/health/readiness`.
- Dependent microservices throwing connection refused (`java.net.ConnectException`) or Feign / WebClient timeouts.

---

## 2. Initial Checks
1. Identify which specific service is reporting unhealthy.
2. Determine environment: Local Docker Compose or Kubernetes cluster.
3. Check container/pod uptime and restart count.
4. Check whether downstream dependencies (MySQL, Redis, Kafka) are healthy.

---

## 3. Commands

### Local (Docker Compose):
```bash
# Check container status
docker compose ps

# Check healthcheck output for the impacted service (e.g. booking-service)
docker inspect --format='{{json .State.Health}}' vibecheck-booking | jq .

# Test Actuator endpoint directly inside Docker network
docker compose exec vibecheck-gateway curl -s http://vibecheck-booking:8084/actuator/health
```

### Kubernetes:
```bash
# Check pod status and restart count in namespace vibecheck
kubectl get pods -n vibecheck -o wide

# Describe impacted pod to view Kubernetes events (OOMKilled, probe failures)
kubectl describe pod -l app.kubernetes.io/name=booking-service -n vibecheck

# Check recent probe failure events
kubectl get events -n vibecheck --sort-by='.lastTimestamp' | grep -i Unhealthy
```

---

## 4. Logs to Inspect

### Local (Docker Compose):
```bash
# Tail last 200 lines and follow logs
docker compose logs --tail=200 -f vibecheck-booking

# Grep for OOM or Fatal errors
docker compose logs vibecheck-booking | grep -iE "OutOfMemoryError|FlywayException|HikariPool|Fatal"
```

### Kubernetes:
```bash
# View current container logs
kubectl logs -n vibecheck -l app.kubernetes.io/name=booking-service --tail=200

# View previous container logs if pod recently crashed/restarted
kubectl logs -n vibecheck -l app.kubernetes.io/name=booking-service --previous --tail=100
```

---

## 5. Metrics to Inspect
In Prometheus / Grafana:
- `up{app="booking-service"}` (1 = UP, 0 = DOWN)
- `jvm_memory_used_bytes{area="heap", app="booking-service"} / jvm_memory_max_bytes`
- `hikaricp_connections_active{pool="HikariPool-1"}` vs `hikaricp_connections_max`
- `http_server_requests_seconds_count{status=~"5..", app="booking-service"}`
- `process_uptime_seconds{app="booking-service"}`

---

## 6. Safe Recovery Procedure

### Scenario A: Process Hanging / Resource Deadlock
1. Restart the individual container/deployment:
   ```bash
   # Docker Compose
   docker compose restart vibecheck-booking

   # Kubernetes
   kubectl rollout restart deployment/booking-service -n vibecheck
   ```
2. Monitor startup logs until Flyway migrations and Spring context load successfully (`Started BookingServiceApplication in X.XXX seconds`).

### Scenario B: Database Connection Exhaustion (HikariPool Empty)
1. Verify if MySQL is overwhelmed:
   ```bash
   docker compose exec vibecheck-mysql mysqladmin -uroot -p"$DB_ROOT_PASSWORD" processlist
   ```
2. Kill idle sleep threads if blocking, or restart MySQL if unrecoverable (see [mysql-recovery.md](file:///c:/Users/acer/Downloads/booking-system/docs/runbooks/mysql-recovery.md)).
3. Restart the microservice once the datastore is accepting connections.

### Scenario C: OutOfMemoryError (OOM)
1. If pod was terminated with exit code 137 (`OOMKilled`):
   ```bash
   # Temporarily increase memory limit in Kubernetes
   kubectl set resources deployment/booking-service -n vibecheck --limits=memory=1536Mi --requests=memory=768Mi
   ```

---

## 7. Verification
1. Verify Actuator health status returns `UP`:
   ```bash
   curl -i http://localhost:8079/actuator/health
   # or Kubernetes port-forward
   kubectl port-forward svc/booking-service 8084:8084 -n vibecheck &
   curl -i http://localhost:8084/actuator/health
   ```
2. Verify probe status in Kubernetes:
   ```bash
   kubectl get pods -l app.kubernetes.io/name=booking-service -n vibecheck
   # STATUS should be Running, READY 1/1
   ```
3. Issue a test read request through the gateway (e.g. `GET /api/v1/movies`).

---

## 8. Rollback Procedure
If the outage occurred immediately after a new deployment or image update:
```bash
# Kubernetes rollback to previous revision
kubectl rollout undo deployment/booking-service -n vibecheck

# Verify rollback completion
kubectl rollout status deployment/booking-service -n vibecheck
```

---

## 9. Escalation Conditions
Escalate to Tech Lead / Infrastructure On-Call immediately if:
- Service continues crashing within 60 seconds of restart (crash looping).
- Downstream data corruption is detected in MySQL tables.
- Core booking path remains down for > 5 minutes during operating hours.
