# Operational Runbook: Booking Failure Spike & Seat Allocation Issues

**Runbook ID:** RB-OPS-005  
**Target:** `booking-service`, `show-service`, Redis Lock Engine  
**Severity:** P1 (Revenue Impacting)

---

## 1. Symptoms
- Prometheus alert: `BookingFailureRateSpike` or `SeatLockContentionHigh` firing.
- Users report error: "Seat is currently locked by another customer", "Seat already booked", or HTTP 409 Conflict.
- Elevated HTTP 500 responses on `POST /api/v1/bookings`.
- Booking status stuck in `PENDING` without transitioning to `CONFIRMED` or `FAILED`.

---

## 2. Initial Checks
1. Identify whether the issue affects all shows/movies or a specific high-demand show.
2. Check Redis distributed lock status: are stale locks held without an active booking session?
3. Check show-service availability: does booking-service get timely seat status updates?
4. Check MySQL database locks in `vibecheck_booking` and `vibecheck_show`.

---

## 3. Commands

### Local (Docker Compose):
```bash
# Check booking-service and show-service status
docker compose ps booking-service show-service redis

# Inspect active Redis locks for seats
docker compose exec redis redis-cli --scan --pattern "lock:seat:*"

# Check pending bookings in MySQL
docker compose exec mysql mysql -uroot -p"$DB_ROOT_PASSWORD" -e "
  SELECT id, user_id, show_id, status, created_at FROM vibecheck_booking.bookings 
  WHERE status = 'PENDING' AND created_at < NOW() - INTERVAL 15 MINUTE;"

# Check seat status in show-service database
docker compose exec mysql mysql -uroot -p"$DB_ROOT_PASSWORD" -e "
  SELECT status, count(*) FROM vibecheck_show.show_seats GROUP BY status;"
```

### Kubernetes:
```bash
# Check booking-service pods
kubectl get pods -n vibecheck -l app.kubernetes.io/name=booking-service

# Query booking service actuator health
kubectl exec -it deployment/booking-service -n vibecheck -- curl -s http://localhost:8084/actuator/health
```

---

## 4. Logs to Inspect
```bash
# Docker Compose: search for lock acquisition or database conflicts
docker compose logs --tail=300 vibecheck-booking | grep -iE "SeatLockAcquisitionException|OptimisticLockingFailureException|LockAcquisitionException|SeatUnavailableException"

# Check show-service logs for seat reservation calls
docker compose logs --tail=200 vibecheck-show | grep -iE "seat|lock|error"
```

---

## 5. Metrics to Inspect
- `vibecheck_booking_created_total{status="FAILED"}`
- `vibecheck_seat_lock_failed_total`
- `http_server_requests_seconds_count{uri="/api/v1/bookings", status="409"}`
- `http_server_requests_seconds_count{uri="/api/v1/bookings", status="500"}`
- `hikaricp_connections_pending{pool="HikariPool-1"}`

---

## 6. Safe Recovery Procedure

### Scenario A: Stale Distributed Locks in Redis
If users abandoned booking flows and Redis keys did not expire as expected (default TTL is 10 minutes):
1. Query key TTLs:
   ```bash
   docker compose exec redis redis-cli TTL "lock:seat:<seat_id>"
   ```
2. If TTL is negative (-1 = no expiry) due to a client disconnect bug, manually evict the orphaned lock:
   ```bash
   docker compose exec redis redis-cli DEL "lock:seat:<seat_id>"
   ```

### Scenario B: Expired Pending Bookings Cleanup
If the automated cleanup scheduler (`BookingCleanupTask`) paused or backed up:
1. Identify orphaned PENDING bookings older than 15 minutes:
   ```bash
   docker compose exec mysql mysql -uroot -p"$DB_ROOT_PASSWORD" -e "
     UPDATE vibecheck_booking.bookings 
     SET status = 'EXPIRED', updated_at = NOW() 
     WHERE status = 'PENDING' AND created_at < NOW() - INTERVAL 15 MINUTE;"
   ```
2. Release corresponding show seats back to `AVAILABLE`:
   ```bash
   docker compose exec mysql mysql -uroot -p"$DB_ROOT_PASSWORD" -e "
     UPDATE vibecheck_show.show_seats 
     SET status = 'AVAILABLE', booking_id = NULL 
     WHERE booking_id IN (SELECT id FROM vibecheck_booking.bookings WHERE status = 'EXPIRED');"
   ```

### Scenario C: High Contention Flash-Crowd Spike
1. Scale up booking-service and show-service:
   ```bash
   kubectl scale deployment booking-service --replicas=5 -n vibecheck
   kubectl scale deployment show-service --replicas=4 -n vibecheck
   ```
2. Verify rate-limiting on API gateway is appropriately throttling non-priority requests.

---

## 7. Verification
1. Attempt a test seat lock and booking creation:
   ```bash
   # Create a valid booking through Gateway API
   curl -X POST http://localhost:8079/api/v1/bookings \
     -H "Authorization: Bearer <test_user_token>" \
     -H "Content-Type: application/json" \
     -d '{"showId": 1, "seatIds": [101]}'
   ```
2. Verify response status is HTTP 201 Created and booking state is `PENDING`.
3. Check Redis lock key is created with valid TTL (~600s).

---

## 8. Rollback Procedure
If a recent deployment altered the seat locking algorithm:
- Roll back booking-service to previous release:
  `kubectl rollout undo deployment/booking-service -n vibecheck`

---

## 9. Escalation Conditions
- Show seats remain stuck in `LOCKED` state despite Redis keys being clear and bookings expired.
- Double-booking occurs (two distinct bookings have identical show seat allocations).
- Booking failure rate exceeds 10% for > 5 minutes continuously.
