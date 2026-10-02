# Milestone 35 — Production Release Runbook

**Release**: VibeCheck v1.0.0-rc.1  
**Date**: 2026-10-02  
**Environment**: Production (Kubernetes) / Local (Docker Compose)

> [!IMPORTANT]
> This runbook is the authoritative step-by-step guide for deploying and validating VibeCheck in production.
> Follow each phase in order. Do NOT skip go/no-go checks.

---

## Phase 0: Pre-Deployment Checklist

- [ ] All Milestone 34 checks pass: `scripts\verify-milestone-34.ps1`
- [ ] Milestone 35 checks pass: `scripts\verify-milestone-35.ps1`
- [ ] Database backups taken for all `vibecheck_*` schemas
- [ ] Docker images built and pushed to registry
- [ ] All required secrets available in Vault / Secret Manager
- [ ] On-call engineer assigned with database access
- [ ] Maintenance window scheduled (30 min minimum)
- [ ] Rollback tested in staging environment

---

## Phase 1: Environment Validation

### 1.1 — Clone and Build
```powershell
git checkout main
git pull origin main
cd booking-system
.\mvnw.cmd compile --batch-mode --no-transfer-progress -q
# Expected: exit 0, no errors
```

### 1.2 — Verify Secrets
```powershell
# Ensure required variables are set (local)
$required = @("DB_ROOT_PASSWORD","JWT_SECRET","INTERNAL_SECURITY_SECRET")
foreach ($v in $required) {
    if (-not [System.Environment]::GetEnvironmentVariable($v)) {
        Write-Error "MISSING: $v"
    } else {
        Write-Host "OK: $v"
    }
}
```

### 1.3 — Verify No Committed Secrets
```powershell
# Run Milestone 35 verification
powershell -File scripts\verify-milestone-35.ps1
# All security checks must PASS before proceeding
```

---

## Phase 2: Docker Compose Deployment (Local / Dev)

### 2.1 — Create .env file
```bash
# Copy and fill in the template
cp .env.template .env
# Edit .env with actual values — NEVER commit this file
nano .env
```

Required `.env` variables:
```env
DB_ROOT_PASSWORD=<strong-password>
JWT_SECRET=<256-bit-hex-string>
INTERNAL_SECURITY_SECRET=<strong-random-string>
PAYMENT_GATEWAY_PROVIDER=MOCK
GATEWAY_PORT=8079
FRONTEND_PORT=80
```

### 2.2 — Start the Stack
```bash
# Pull latest images (if using pre-built)
docker-compose pull

# Or build locally
docker-compose build --no-cache

# Start all services
docker-compose up -d

# Watch startup logs
docker-compose logs -f --tail=50
```

### 2.3 — Service Health Verification
```bash
# Wait for all services to become healthy (up to 3 minutes)
echo "Waiting for gateway..."
until curl -sf http://localhost:8079/actuator/health | grep -q '"status":"UP"'; do
  sleep 5; echo -n "."
done
echo ""
echo "Gateway is UP"

# Verify all containers are healthy
docker-compose ps
# All services should show 'healthy'
```

### 2.4 — Go / No-Go Decision
| Check | Expected | Verdict |
|---|---|---|
| `docker-compose ps` | All services `healthy` | GO |
| `GET /actuator/health` | `{"status":"UP"}` | GO |
| Any container in `Exit` state | — | NO-GO → see Troubleshooting |

---

## Phase 3: Kubernetes / Helm Deployment (Production)

### 3.1 — Prepare Kubernetes Secrets
```bash
# Option A: External Secrets Operator (recommended for production)
# Set externalSecrets.enabled=true in values-prod.yaml
# Populate secrets from Vault/AWS/GCP before deployment

# Option B: Inline secrets (CI / minikube only)
kubectl create secret generic vibecheck-secrets \
  --from-literal=JWT_SECRET="$(cat /secure/jwt.secret)" \
  --from-literal=DB_ROOT_PASSWORD="$(cat /secure/db.password)" \
  --from-literal=INTERNAL_SECURITY_SECRET="$(cat /secure/internal.secret)" \
  -n vibecheck
```

### 3.2 — Helm Lint Validation
```bash
# Validate all Helm charts
helm lint k8s/helm/vibecheck
helm lint k8s/helm/vibecheck -f k8s/helm/vibecheck/values-prod.yaml
helm template vibecheck k8s/helm/vibecheck > /dev/null
# All must succeed with 0 failures
```

### 3.3 — Deploy with Helm
```bash
# First deployment
helm install vibecheck k8s/helm/vibecheck \
  -f k8s/helm/vibecheck/values-prod.yaml \
  -n vibecheck \
  --create-namespace \
  --wait \
  --timeout 10m

# Subsequent upgrades
helm upgrade vibecheck k8s/helm/vibecheck \
  -f k8s/helm/vibecheck/values-prod.yaml \
  -n vibecheck \
  --wait \
  --timeout 10m
```

### 3.4 — Verify Kubernetes Rollout
```bash
# Check all deployments are ready
kubectl get deployments -n vibecheck
kubectl get pods -n vibecheck

# Verify probes are passing
kubectl describe deployment gateway-service -n vibecheck | grep -A5 "Conditions"

# Check services are reachable
kubectl get services -n vibecheck
```

### 3.5 — Production Go / No-Go
| Check | Expected | Verdict |
|---|---|---|
| All pods in `Running` state | Yes | GO |
| No `CrashLoopBackOff` | None | GO |
| Readiness probe passing | Yes | GO |
| Gateway health endpoint | 200 UP | GO |
| Ingress TLS certificate | Valid | GO |

---

## Phase 4: Database Migration Verification

Flyway runs automatically on service startup. Verify with:

```bash
# Check migration logs (Docker Compose)
docker-compose logs auth-service | grep -i flyway
# Expected: "Successfully applied N migration(s)"

# For Kubernetes
kubectl logs deployment/auth-service -n vibecheck | grep -i flyway
```

**Expected log output (all services):**
```
Successfully applied 1 migration(s) to schema `vibecheck_auth`
Successfully applied 7 migration(s) to schema `vibecheck_booking`
Successfully applied 7 migration(s) to schema `vibecheck_payment`
```

> [!WARNING]
> If you see `Flyway detected failed migration` — STOP and rollback immediately.
> Never run with `repair` in production without DBA review.

---

## Phase 5: E2E Smoke Test

Run immediately after deployment:

```powershell
# Full Milestone 35 validation (includes live E2E if gateway is up)
powershell -File scripts\verify-milestone-35.ps1 -GatewayUrl "http://localhost:8079"

# Or for Kubernetes with ingress
powershell -File scripts\verify-milestone-35.ps1 -GatewayUrl "https://api.vibecheck.com"
```

### Manual E2E Walkthrough
1. Open browser: `http://localhost/` (Docker) or `https://vibecheck.com` (production)
2. Click **Sign Up** → register a new account
3. Sign in with new credentials
4. Browse movies → select a movie
5. Select a showtime
6. Select seats → click **Proceed to Payment**
7. Complete payment (MOCK: any amount)
8. Verify booking confirmation and QR code ticket

---

## Phase 6: Observability Verification

### Metrics
```bash
# Gateway Prometheus metrics
curl http://localhost:8079/actuator/metrics/http.server.requests

# Circuit breaker state
curl http://localhost:8079/actuator/metrics/resilience4j.circuitbreaker.state
```

### Logs
```bash
# Follow gateway logs for errors
docker-compose logs -f gateway-service | grep -E "ERROR|WARN|circuit"

# Check correlation IDs are flowing
docker-compose logs gateway-service | grep "X-Correlation-ID"
```

### Alerts to configure
| Alert | Threshold | Severity |
|---|---|---|
| HTTP 5xx rate | > 1% per minute | CRITICAL |
| Circuit breaker OPEN | Any service | HIGH |
| Kafka consumer lag | > 1000 messages | HIGH |
| Redis key expiry rate | Anomalous spike | MEDIUM |
| MySQL connection pool exhaustion | > 90% | HIGH |
| JVM heap > 85% | Any service | MEDIUM |

---

## Phase 7: Post-Deployment Verification (15 min soak)

Monitor for 15 minutes post-deployment:

- [ ] Error rate < 0.1%
- [ ] P99 latency < 2s on `/api/v1/bookings`
- [ ] No OOM kills in any pod
- [ ] Kafka consumer lag at zero
- [ ] Redis hit rate > 90% on seat lock reads

---

## Troubleshooting

### Service won't start
```bash
# Check container logs
docker-compose logs [service-name] | tail -50

# Common cause: database not ready
# Fix: service depends_on health check, wait and retry
docker-compose restart [service-name]
```

### Flyway migration failure
```bash
# Read the failed migration error carefully
docker-compose logs auth-service | grep -A20 "Flyway"

# Do NOT use repair without DBA approval
# Restore from backup and investigate the migration script
```

### Gateway circuit breaker OPEN
```bash
# Check downstream service health
curl http://localhost:8080/actuator/health  # auth-service
curl http://localhost:8084/actuator/health  # booking-service

# Force close circuit (admin only)
curl -X POST http://localhost:8079/actuator/circuitbreakers/authService/reset
```

### Redis connection refused
```bash
# Verify Redis is running
docker-compose ps redis
redis-cli -h localhost ping  # expect: PONG

# Seat locking will fail gracefully (409s to clients)
# Restart Redis if down
docker-compose restart redis
```

### 401 errors on all requests
```bash
# JWT_SECRET mismatch between auth-service and gateway-service
# Both must use identical JWT_SECRET value
# Restart both services after fixing:
docker-compose restart auth-service gateway-service
```

---

## Rollback Procedure

### Docker Compose Rollback
```bash
docker-compose down

# Restore previous docker-compose.yml (git revert or backup)
git checkout HEAD~1 -- docker-compose.yml

docker-compose up -d
```

### Kubernetes Rollback
```bash
# Immediate rollback to previous revision
for svc in gateway auth movie theatre show booking payment notification; do
  kubectl rollout undo deployment/${svc}-service -n vibecheck
done

# Monitor rollback
kubectl rollout status deployment/gateway-service -n vibecheck
```

### Helm Rollback
```bash
helm history vibecheck -n vibecheck
helm rollback vibecheck [PREVIOUS_REVISION] -n vibecheck --wait
```

---

## Contact & Escalation

| Role | Responsibility |
|---|---|
| Release Engineer | Executes this runbook |
| DBA | Any database migration issue |
| Platform/Infra | Kubernetes / networking issues |
| Payment Team | Any payment gateway issues |
| On-Call | Incident response if smoke tests fail |

**Decision point**: If any Phase 5 E2E check fails, execute rollback immediately.
Do not attempt to hotfix in production during a release window.
