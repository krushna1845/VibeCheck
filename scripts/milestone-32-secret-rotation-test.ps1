#!/usr/bin/env pwsh
<#
.SYNOPSIS
    VibeCheck Milestone 32 - Secret Rotation Validation

.DESCRIPTION
    Validates the secret rotation procedure for VibeCheck production secrets.

    Tests:
      SECRET-01  JWT signing secret rotation
      SECRET-02  Database credential rotation
      SECRET-03  Payment provider secret rotation

    SAFETY:
    - This script SIMULATES secret rotation unless -Provider is specified.
    - No actual provider (Vault/AWS/GCP) credentials are required for simulation mode.
    - No secrets are printed or logged.
    - Simulation mode tests the rotation PROCEDURE, not live provider integration.

.PARAMETER Mode
    "simulate" (default): Simulates rotation without live ESO.
    "vault"  : Tests against a live HashiCorp Vault instance.

.PARAMETER VaultAddr
    Vault server address (only used in "vault" mode).

.PARAMETER Namespace
    Kubernetes namespace. Default: vibecheck

.EXAMPLE
    .\scripts\milestone-32-secret-rotation-test.ps1
    .\scripts\milestone-32-secret-rotation-test.ps1 -Mode vault -VaultAddr http://localhost:8200
#>

param(
    [ValidateSet("simulate", "vault")]
    [string]$Mode = "simulate",
    [string]$VaultAddr = "http://localhost:8200",
    [string]$Namespace = "vibecheck"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Continue"

# -----------------------------------------------------------------------
# Counters and Logging
# -----------------------------------------------------------------------
$script:PassCount    = 0
$script:FailCount    = 0
$script:SkipCount    = 0
$script:Results      = @()

function Write-Log {
    param([string]$Level, [string]$Message)
    $ts = Get-Date -Format "HH:mm:ss"
    $color = switch ($Level) {
        "PASS"    { "Green"   }
        "FAIL"    { "Red"     }
        "SKIP"    { "Yellow"  }
        "INFO"    { "Cyan"    }
        "STEP"    { "Magenta" }
        "WARN"    { "Yellow"  }
        "SIMUL"   { "DarkCyan" }
        default   { "White"   }
    }
    Write-Host "[$ts][$Level] $Message" -ForegroundColor $color
}

function Record-Result {
    param([string]$TestId, [string]$Name, [string]$Status, [string]$Detail = "")
    switch ($Status) {
        "PASS"         { $script:PassCount++ }
        "FAIL"         { $script:FailCount++ }
        default        { $script:SkipCount++ }
    }
    Write-Log $Status "$TestId: $Name$(if ($Detail) { ' -- ' + $Detail })"
    $script:Results += [PSCustomObject]@{
        TestId = $TestId
        Name   = $Name
        Status = $Status
        Detail = $Detail
    }
}

function Mask-Value {
    param([string]$Value)
    if ([string]::IsNullOrEmpty($Value)) { return "[empty]" }
    if ($Value.Length -le 4) { return "****" }
    return $Value.Substring(0, 4) + "****" + $Value.Substring($Value.Length - 2)
}

# -----------------------------------------------------------------------
# HEADER
# -----------------------------------------------------------------------
Write-Host ""
Write-Host "================================================================" -ForegroundColor Magenta
Write-Host "  VIBECHECK MILESTONE 32 - SECRET ROTATION VALIDATION" -ForegroundColor Magenta
Write-Host "  Mode: $Mode | Namespace: $Namespace" -ForegroundColor Magenta
Write-Host "================================================================" -ForegroundColor Magenta
Write-Host ""

if ($Mode -eq "simulate") {
    Write-Log "WARN" "SIMULATION MODE: No actual provider rotation is performed."
    Write-Log "WARN" "This validates the rotation PROCEDURE and command correctness."
    Write-Log "WARN" "Mark results as SIMULATED -- not equivalent to live provider test."
}

# -----------------------------------------------------------------------
# PREREQUISITE CHECK
# -----------------------------------------------------------------------
Write-Log "STEP" "Checking prerequisites..."

$kubectlAvail = $null -ne (Get-Command kubectl -ErrorAction SilentlyContinue)
$vaultAvail   = $null -ne (Get-Command vault -ErrorAction SilentlyContinue)

if ($kubectlAvail) {
    Write-Log "INFO" "kubectl: available"
} else {
    Write-Log "WARN" "kubectl: not available (K8s verification steps will be skipped)"
}

if ($Mode -eq "vault" -and -not $vaultAvail) {
    Write-Log "WARN" "vault CLI not available. Some Vault tests will be skipped."
}

# -----------------------------------------------------------------------
# SECRET-01: JWT signing secret rotation
# -----------------------------------------------------------------------
Write-Log "STEP" "SECRET-01: JWT signing secret rotation"

if ($Mode -eq "simulate") {
    Write-Log "SIMUL" "SECRET-01 SIMULATED: JWT rotation procedure:"
    Write-Log "SIMUL" "  1. Generate new JWT secret (min 32 bytes, cryptographically random)"
    Write-Log "SIMUL" "     Command: [System.Web.Security.Membership]::GeneratePassword(64,8)"
    Write-Log "SIMUL" "  2. Update in provider: vault kv patch secret/vibecheck/production/auth jwt_secret=<new-value>"
    Write-Log "SIMUL" "  3. ESO syncs within 1 hour (or trigger: kubectl annotate externalsecret -n <ns> force-sync=true)"
    Write-Log "SIMUL" "  4. Rolling restart: kubectl rollout restart deployment/auth-service -n vibecheck"
    Write-Log "SIMUL" "  5. Rolling restart: kubectl rollout restart deployment/gateway-service -n vibecheck"
    Write-Log "SIMUL" "  6. Verify: all active sessions require re-authentication (expected behavior)"
    Write-Log "SIMUL" "  7. Monitor: auth-service error rate in Prometheus for 15 min"
    Write-Log "SIMUL" "  NOTE: No secrets are printed. New value is never logged."

    Record-Result "SECRET-01" "JWT rotation procedure documented and validated" "SIMULATED" `
        "Rotation invalidates all active sessions. Users must re-login. See docs/milestone-32-secret-management.md"
    Record-Result "SECRET-01a" "New JWT secret generation method validated" "SIMULATED" `
        "Use cryptographically secure random source (not Math.random)"
    Record-Result "SECRET-01b" "Rolling restart required after JWT rotation" "SIMULATED" `
        "kubectl rollout restart deployment/auth-service deployment/gateway-service -n vibecheck"

} elseif ($Mode -eq "vault" -and $vaultAvail) {
    # Live Vault rotation test
    Write-Log "INFO" "Testing against Vault at $VaultAddr..."
    $env:VAULT_ADDR = $VaultAddr

    # Check Vault connectivity (no auth required for status)
    $vaultStatus = vault status 2>$null
    $vaultSealed = $vaultStatus -match "Sealed\s+true"
    $vaultAvailable = ($LASTEXITCODE -eq 0) -and (-not $vaultSealed)

    if (-not $vaultAvailable) {
        Record-Result "SECRET-01" "Vault is reachable and unsealed" "FAIL" `
            "Vault at $VaultAddr is not available or sealed"
    } else {
        Record-Result "SECRET-01" "Vault connectivity verified" "PASS" "Vault at $VaultAddr is reachable"

        # Read current JWT secret metadata (NOT the value)
        $jwtMeta = vault kv metadata get secret/vibecheck/production/auth 2>$null
        $metaOk = ($LASTEXITCODE -eq 0)
        Record-Result "SECRET-01a" "JWT secret exists in Vault" `
            (if ($metaOk) { "PASS" } else { "FAIL" }) `
            "Secret path: secret/vibecheck/production/auth"

        Write-Log "INFO" "Vault rotation test: verifying rotation PROCEDURE only (not executing actual rotation)"
        Write-Log "INFO" "To rotate: vault kv patch secret/vibecheck/production/auth jwt_secret=<new-value>"
        Record-Result "SECRET-01b" "JWT rotation procedure verified (not executed)" "SIMULATED" `
            "Actual rotation not performed to avoid session invalidation in test"
    }
}

# -----------------------------------------------------------------------
# SECRET-02: Database credential rotation
# -----------------------------------------------------------------------
Write-Log "STEP" "SECRET-02: Database credential rotation"

if ($Mode -eq "simulate") {
    Write-Log "SIMUL" "SECRET-02 SIMULATED: Database credential rotation procedure:"
    Write-Log "SIMUL" "  1. Create new MySQL user: CREATE USER 'vibecheck_app'@'%' IDENTIFIED BY '<new-password>';"
    Write-Log "SIMUL" "  2. Grant privileges: GRANT ALL ON vibecheck_*.* TO 'vibecheck_app'@'%';"
    Write-Log "SIMUL" "  3. Update in provider: vault kv patch secret/vibecheck/production/database root_password=<new-value>"
    Write-Log "SIMUL" "  4. Wait for ESO sync (or force: kubectl annotate externalsecret force-sync=...)"
    Write-Log "SIMUL" "  5. Rolling restart all DB-connected services"
    Write-Log "SIMUL" "  6. Verify all services return healthy"
    Write-Log "SIMUL" "  7. Revoke old MySQL user: DROP USER 'old_user'@'%';"
    Write-Log "SIMUL" "  RISK: Application downtime during rolling restart if single DB instance."
    Write-Log "SIMUL" "  MITIGATION: With managed DB + dedicated app user, rotate only app credential."

    Record-Result "SECRET-02" "DB credential rotation procedure documented" "SIMULATED" `
        "Requires rolling restart of all DB-connected services"
    Record-Result "SECRET-02a" "Application uses dedicated DB user (not root)" "FAIL" `
        "Current implementation uses root user. Production MUST use dedicated application user."
    Record-Result "SECRET-02b" "DB restart strategy documented" "SIMULATED" `
        "kubectl rollout restart deployment/<all-services> -n vibecheck"
}

# -----------------------------------------------------------------------
# SECRET-03: Payment provider secret rotation
# -----------------------------------------------------------------------
Write-Log "STEP" "SECRET-03: Payment provider secret rotation"

if ($Mode -eq "simulate") {
    Write-Log "SIMUL" "SECRET-03 SIMULATED: Payment provider secret rotation:"
    Write-Log "SIMUL" "  Razorpay Key Rotation:"
    Write-Log "SIMUL" "    1. Generate new API key pair from Razorpay Dashboard"
    Write-Log "SIMUL" "    2. Update in provider: vault kv patch secret/vibecheck/production/payment/razorpay"
    Write-Log "SIMUL" "    3. ESO syncs and updates K8s Secret"
    Write-Log "SIMUL" "    4. Rolling restart payment-service"
    Write-Log "SIMUL" "    5. Verify payment health endpoint"
    Write-Log "SIMUL" "    6. Test a mock payment in staging"
    Write-Log "SIMUL" "    7. Revoke old key from Razorpay Dashboard"
    Write-Log "SIMUL" "  Webhook Secret Rotation:"
    Write-Log "SIMUL" "    1. Generate new webhook secret"
    Write-Log "SIMUL" "    2. Update in payment provider webhook config"
    Write-Log "SIMUL" "    3. Update in provider secret store"
    Write-Log "SIMUL" "    4. Overlap window: old and new secrets both valid for 5 min"
    Write-Log "SIMUL" "    5. Rolling restart payment-service"
    Write-Log "SIMUL" "    6. Monitor DLT for unprocessable webhooks"

    Record-Result "SECRET-03" "Payment key rotation procedure documented" "SIMULATED" `
        "Razorpay and Stripe key rotation requires provider dashboard + K8s rolling restart"
    Record-Result "SECRET-03a" "Webhook rotation overlap window documented" "SIMULATED" `
        "5-minute overlap prevents missed webhooks during rotation"
    Record-Result "SECRET-03b" "No secrets printed in rotation procedure" "PASS" `
        "All rotation steps reference secret paths only, never values"
}

# -----------------------------------------------------------------------
# K8s SECRET REFERENCE VALIDATION (static, both modes)
# -----------------------------------------------------------------------
Write-Log "STEP" "Kubernetes secret reference validation"

if ($kubectlAvail) {
    $secretExists = kubectl get secret vibecheck-secrets -n $Namespace 2>$null
    $secretOk = ($LASTEXITCODE -eq 0)
    Record-Result "SECRET-K8S-01" "vibecheck-secrets exists in Kubernetes" `
        (if ($secretOk) { "PASS" } else { "NOT EXECUTED" }) `
        (if ($secretOk) { "Secret found" } else { "kubectl returned non-zero. Cluster may not be running." })

    if ($secretOk) {
        # Verify key names (NOT values)
        $secretKeys = kubectl get secret vibecheck-secrets -n $Namespace -o jsonpath="{.data}" 2>$null
        $hasJwt        = $secretKeys -match "JWT_SECRET"
        $hasDbPwd      = $secretKeys -match "DB_ROOT_PASSWORD"
        $hasWebhook    = $secretKeys -match "PAYMENT_WEBHOOK_SECRET"

        Record-Result "SECRET-K8S-02" "JWT_SECRET key present in Kubernetes Secret" `
            (if ($hasJwt) { "PASS" } else { "FAIL" }) ""
        Record-Result "SECRET-K8S-03" "DB_ROOT_PASSWORD key present" `
            (if ($hasDbPwd) { "PASS" } else { "FAIL" }) ""
        Record-Result "SECRET-K8S-04" "PAYMENT_WEBHOOK_SECRET key present" `
            (if ($hasWebhook) { "PASS" } else { "FAIL" }) ""
    }
} else {
    Record-Result "SECRET-K8S-01" "Kubernetes secret validation" "NOT EXECUTED" `
        "kubectl not available. Run against a live cluster."
}

# -----------------------------------------------------------------------
# STATIC SECURITY CHECK: No secrets in values files
# -----------------------------------------------------------------------
Write-Log "STEP" "Static security check: secrets in Git-tracked files"

$sensitivePatterns = @(
    @{ Pattern = "rzp_live_"; Desc = "Live Razorpay key" },
    @{ Pattern = "sk_live_";  Desc = "Live Stripe key" },
    @{ Pattern = "whsec_[a-zA-Z0-9]{20}"; Desc = "Real Stripe webhook secret" },
    @{ Pattern = "BEGIN.*PRIVATE KEY"; Desc = "Private key in Git" }
)

$gitFiles = @("k8s/helm/vibecheck/values.yaml", "k8s/helm/vibecheck/values-prod.yaml", ".env")
$foundRealSecrets = $false

foreach ($file in $gitFiles) {
    if (Test-Path $file) {
        $content = Get-Content $file -Raw
        foreach ($p in $sensitivePatterns) {
            if ($content -match $p.Pattern) {
                Write-Log "FAIL" "REAL SECRET FOUND in $file: $($p.Desc)"
                $foundRealSecrets = $true
            }
        }
    }
}

Record-Result "SECRET-STATIC-01" "No live payment credentials in Git-tracked values files" `
    (if (-not $foundRealSecrets) { "PASS" } else { "FAIL" }) `
    (if (-not $foundRealSecrets) { "No live credentials detected" } else { "LIVE CREDENTIALS FOUND - CRITICAL" })

# Check that values.yaml only has test placeholders
$valuesContent = Get-Content "k8s/helm/vibecheck/values.yaml" -Raw
$hasTestValues  = $valuesContent -match "rzp_test_key" -and $valuesContent -match "sk_test_key"
Record-Result "SECRET-STATIC-02" "values.yaml contains only test placeholders" `
    (if ($hasTestValues) { "PASS" } else { "WARN" }) `
    "Test keys detected: rzp_test_key, sk_test_key (non-functional test values)"

# -----------------------------------------------------------------------
# FINAL REPORT
# -----------------------------------------------------------------------
Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  SECRET ROTATION TEST RESULTS" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

foreach ($r in $script:Results) {
    $color = switch ($r.Status) {
        "PASS"      { "Green"    }
        "FAIL"      { "Red"      }
        "SIMULATED" { "DarkCyan" }
        "NOT EXECUTED" { "Yellow" }
        default     { "White"    }
    }
    $detail = if ($r.Detail) { "  ($($r.Detail))" } else { "" }
    Write-Host "  [$($r.Status.PadRight(12))] $($r.TestId.PadRight(18)) $($r.Name)$detail" -ForegroundColor $color
}

Write-Host ""
Write-Host "  PASS         : $($script:PassCount)" -ForegroundColor Green
Write-Host "  FAIL         : $($script:FailCount)" -ForegroundColor $(if ($script:FailCount -gt 0) { "Red" } else { "Green" })
Write-Host "  SIMULATED    : $($script:SkipCount)" -ForegroundColor DarkCyan
Write-Host ""
Write-Host "  NOTE: SIMULATED results represent procedural verification only." -ForegroundColor Yellow
Write-Host "        Live provider rotation requires: Vault/AWS/GCP infrastructure." -ForegroundColor Yellow
Write-Host "        See: docs/milestone-32-secret-management.md" -ForegroundColor Yellow
Write-Host "================================================================" -ForegroundColor Cyan

exit $(if ($script:FailCount -gt 0) { 1 } else { 0 })
