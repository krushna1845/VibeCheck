# Milestone 32 — Production Secret Management

**Date:** 2026-09-28
**Status:** ARCHITECTURE DEFINED; MANIFESTS IMPLEMENTED; NOT EXECUTED (no live ESO environment)

---

## 1. Secret Flow Architecture

### Current (Local/CI — INSECURE for production)

```
values.yaml (plaintext secrets in Git)
    |
    v
Helm template renders Kubernetes Secret "vibecheck-secrets"
    |
    v
Pod environment (envFrom secretRef)
    |
    v
Spring Boot application
```

**Problem:** All secret values are committed to the Git repository in `values.yaml`
and `values-minikube.yaml`. This is explicitly identified as a production blocker
in the Milestone 31 CONDITIONAL verdict.

### Target (Production — External Secrets Operator)

```
External Secret Provider (Vault / AWS SM / GCP SM / Azure KV)
    |
    | Provider auth: Kubernetes SA / IRSA / Workload Identity
    v
External Secrets Operator (K8s controller)
    |
    | ExternalSecret CR reconcile loop (every 1h)
    v
Kubernetes Secret "vibecheck-secrets" (created/updated by ESO)
    |
    v
Pod environment (envFrom secretRef — unchanged)
    |
    v
Spring Boot application (unchanged)
```

Application pods and Spring Boot code are UNCHANGED by this migration.
Only the secret population mechanism changes.

---

## 2. Components Deployed

### 2.1 ExternalSecret CR
Location: `k8s/helm/vibecheck/templates/secrets/external-secret.yaml`
- Rendered when `externalSecrets.enabled=true`
- Maps provider secret paths to Kubernetes Secret keys
- Refresh interval: 1 hour (configurable)
- Deletion policy: Retain (prevents accidental data loss on CR deletion)

### 2.2 ClusterSecretStore CR
Location: `k8s/helm/vibecheck/templates/secrets/cluster-secret-store.yaml`
- Rendered when `externalSecrets.enabled=true AND externalSecrets.deployStore=true`
- Supports providers: vault, aws, gcpsm, azurekv
- Provider authentication uses workload identity (no credentials in manifests)

### 2.3 Modified Inline Secret Template
Location: `k8s/helm/vibecheck/templates/secrets/vibecheck-secrets.yaml`
- Now conditional: only renders when `externalSecrets.enabled=false`
- When ESO is enabled, the ExternalSecret creates vibecheck-secrets instead
- Comment added: "PLACEHOLDER defaults for local/CI only"

---

## 3. Provider Configuration

### 3.1 HashiCorp Vault (Recommended for self-hosted)

**Setup:**
```bash
# Install Vault
helm repo add hashicorp https://helm.releases.hashicorp.com
helm install vault hashicorp/vault -n vault --create-namespace

# Enable Kubernetes auth
vault auth enable kubernetes
vault write auth/kubernetes/config \
  kubernetes_host="https://kubernetes.default.svc.cluster.local:443"

# Create policy
vault policy write vibecheck-policy - <<EOF
path "secret/data/vibecheck/*" { capabilities = ["read"] }
EOF

# Create role
vault write auth/kubernetes/role/vibecheck \
  bound_service_account_names=vibecheck-eso-sa \
  bound_service_account_namespaces=vibecheck \
  policies=vibecheck-policy \
  ttl=1h

# Store secrets (replace with real values - never commit these)
vault kv put secret/vibecheck/production/database root_password="<REAL_PASSWORD>"
vault kv put secret/vibecheck/production/auth \
  jwt_secret="<REAL_JWT_SECRET>" \
  internal_security_secret="<REAL_INTERNAL_SECRET>"
vault kv put secret/vibecheck/production/payment \
  webhook_secret="<REAL_WEBHOOK_SECRET>"
vault kv put secret/vibecheck/production/payment/razorpay \
  key_id="<REAL_KEY_ID>" \
  key_secret="<REAL_KEY_SECRET>" \
  webhook_secret="<REAL_WEBHOOK_SECRET>"
vault kv put secret/vibecheck/production/payment/stripe \
  api_key="<REAL_API_KEY>" \
  webhook_secret="<REAL_WEBHOOK_SECRET>"
vault kv put secret/vibecheck/production/notification/mail \
  host="smtp.example.com" \
  port="587" \
  username="<REAL_USERNAME>" \
  password="<REAL_PASSWORD>" \
  from_address="noreply@vibecheck.com"
```

**Values override for Vault:**
```yaml
# values-production.yaml (NEVER committed with real values)
externalSecrets:
  enabled: true
  deployStore: true
  provider: vault
  vault:
    address: "http://vault.vault.svc.cluster.local:8200"
    role: "vibecheck"
    serviceAccount: "vibecheck-eso-sa"
```

### 3.2 AWS Secrets Manager (Recommended for AWS workloads)

**Setup:**
```bash
# Create secrets (never commit these)
aws secretsmanager create-secret \
  --name vibecheck/production/database \
  --secret-string '{"root_password":"<REAL_PASSWORD>"}'

# IRSA: Create IAM role with policy to read vibecheck/* secrets
# Annotate service account with IAM role ARN
kubectl annotate serviceaccount vibecheck-eso-sa -n vibecheck \
  eks.amazonaws.com/role-arn=arn:aws:iam::ACCOUNT:role/vibecheck-eso-role
```

**Values override for AWS:**
```yaml
externalSecrets:
  enabled: true
  deployStore: true
  provider: aws
  aws:
    region: ap-south-1
    serviceAccount: vibecheck-eso-sa
```

---

## 4. Secret Categories and Rotation Policy

| Secret | Category | Rotation Frequency | Rotation Impact |
|---|---|---|---|
| DB_ROOT_PASSWORD | Database | Quarterly | Requires app restart |
| JWT_SECRET | Authentication | Semi-annually | All sessions invalidated |
| INTERNAL_SECURITY_SECRET | Service auth | Semi-annually | Gateway restart needed |
| RAZORPAY_KEY_ID/SECRET | Payment | Per Razorpay policy | Requires app restart |
| STRIPE_API_KEY | Payment | Per Stripe policy | Requires app restart |
| MAIL_PASSWORD | Notification | Annually | Requires app restart |
| Webhook secrets | Payment | Per provider policy | Requires app restart |

### Rotation Procedure (ESO-managed)
1. Update secret value in the external provider (Vault/AWS/GCP)
2. ESO detects change on next refresh interval (up to 1 hour)
3. ESO updates Kubernetes Secret `vibecheck-secrets`
4. Application reads secrets from environment at startup -- restart required
5. Trigger rolling restart: `kubectl rollout restart deployment/<service-name> -n vibecheck`

---

## 5. Secrets NOT to Commit to Git

The following must NEVER appear in any Git-committed file:

```
- Database passwords
- JWT signing secrets (real production values)
- Payment provider API keys (Razorpay, Stripe)
- Payment webhook validation secrets
- Mail server credentials
- Cloud provider credentials (AWS access keys, GCP service account keys)
- Vault tokens
- Any value from a .env file containing real credentials
```

### Git History Verification
```bash
# Check for committed secrets (run periodically)
git log --all --full-history -- '*.env' | head -20
git grep -i "password" -- '*.yaml' | grep -v "ChangeMeInProd\|test-webhook\|rzp_test\|sk_test\|whsec_test\|mailpassword"
```

---

## 6. Production Deployment Checklist

Before deploying to production:

- [ ] ESO installed in cluster (`helm install external-secrets ...`)
- [ ] ClusterSecretStore deployed and provider reachable
- [ ] All secrets populated in provider (no empty values)
- [ ] `externalSecrets.enabled: true` in production values
- [ ] `secrets.*` block removed or zeroed in production values (not committed)
- [ ] ExternalSecret synced: `kubectl get externalsecret -n vibecheck`
- [ ] vibecheck-secrets Kubernetes Secret exists: `kubectl get secret vibecheck-secrets -n vibecheck`
- [ ] All pods using secretRef can start and read env vars
- [ ] Health endpoints return UP for all services

---

## 7. Verification (Static)

```bash
# Verify ExternalSecret would render correctly
helm template vibecheck k8s/helm/vibecheck \
  --set externalSecrets.enabled=true \
  --set externalSecrets.deployStore=true \
  --set externalSecrets.provider=vault \
  | grep -A 50 "ExternalSecret"

# Verify inline secret is NOT rendered when ESO enabled
helm template vibecheck k8s/helm/vibecheck \
  --set externalSecrets.enabled=true \
  | grep "kind: Secret" | wc -l  # Should be 0
```

---

## 8. Known Limitations

1. **No live ESO environment available** -- all verification is static (helm template). Runtime verification is marked NOT EXECUTED.
2. **JWT secret rotation invalidates all sessions** -- users must re-login. Implement dual-key JWT rotation for zero-downtime rotation (future milestone).
3. **DB password rotation requires app restart** -- Spring Boot reads DB password at startup. Dynamic DB credential rotation (e.g., Vault dynamic secrets) requires code changes (future milestone).
4. **values.yaml contains test placeholder secrets** -- these are intentionally non-functional test values. They must be replaced with `externalSecrets.enabled=true` in any non-local deployment.
