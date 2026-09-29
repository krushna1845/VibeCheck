#!/usr/bin/env pwsh
<#
.SYNOPSIS
    VibeCheck Milestone 32 — Automated Verification Script

.DESCRIPTION
    Verifies Milestone 32 deliverables systematically:

    1.  Helm lint (default values)
    2.  Helm lint (minikube values)
    3.  Helm lint (production values)
    4.  Helm template rendering
    5.  Kubernetes manifest validation
    6.  Dockerfile non-root configuration
    7.  Resource requests/limits configuration
    8.  HPA configuration
    9.  PDB configuration
    10. Network policy presence
    11. Secret reference validation (no live credentials in Git)
    12. Kafka HA configuration (3 brokers, RF>=3, min ISR>=2)
    13. Database configuration
    14. Redis configuration
    15. Health probe configuration
    16. Graceful shutdown configuration
    17. Observability configuration
    18. ExternalSecrets manifests present
    19. Maven tests $(if mvnw.cmd available)
    20. Milestone 32 documentation completeness

.EXAMPLE
    .\scripts\verify-milestone-32.ps1
    .\scripts\verify-milestone-32.ps1 -SkipMaven  # Skip slow Maven tests
#>

param(
    [switch]$SkipMaven,
    [switch]$Verbose
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Continue"

# -----------------------------------------------------------------------
# Counters and State
# -----------------------------------------------------------------------
$script:PassCount    = 0
$script:FailCount    = 0
$script:PartialCount = 0
$script:SkipCount    = 0
$script:Results      = @()
$StartTime           = Get-Date

function Write-Log {
    param([string]$Level, [string]$Message)
    $ts = Get-Date -Format "HH:mm:ss"
    $color = switch ($Level) {
        "PASS"    { "Green"   }
        "FAIL"    { "Red"     }
        "PARTIAL" { "Yellow"  }
        "SKIP"    { "DarkGray"}
        "INFO"    { "Cyan"    }
        "STEP"    { "Magenta" }
        default   { "White"   }
    }
    Write-Host "[$ts][$Level] $Message" -ForegroundColor $color
}

function Record-Result {
    param([string]$Id, [string]$Name, [string]$Status, [string]$Detail = "")
    switch ($Status) {
        "PASS"    { $script:PassCount++ }
        "FAIL"    { $script:FailCount++ }
        "PARTIAL" { $script:PartialCount++ }
        "SKIP"    { $script:SkipCount++ }
    }
    $icon = switch ($Status) {
        "PASS"    { "[PASS]   " }
        "FAIL"    { "[FAIL]   " }
        "PARTIAL" { "[PARTIAL]" }
        "SKIP"    { "[SKIP]   " }
        default   { "[???]    " }
    }
    $color = switch ($Status) {
        "PASS"    { "Green"  }
        "FAIL"    { "Red"    }
        "PARTIAL" { "Yellow" }
        "SKIP"    { "DarkGray" }
        default   { "White"  }
    }
    $detail_str = if ($Detail) { " -- $Detail" } else { "" }
    Write-Host "  $icon $Id $Name$detail_str" -ForegroundColor $color
    $script:Results += [PSCustomObject]@{
        Id = $Id; Name = $Name; Status = $Status; Detail = $Detail
    }
}

function Test-FileExists {
    param([string]$Path, [string]$Id, [string]$Name)
    $exists = Test-Path $Path
    Record-Result $Id $Name $(if ($exists) { "PASS" } else { "FAIL" }) `
        $(if (-not $exists) { "Missing: $Path" } else { "" })
    return $exists
}

function Test-FileContains {
    param([string]$Path, [string]$Pattern, [string]$Id, [string]$Name)
    if (-not (Test-Path $Path)) {
        Record-Result $Id $Name "FAIL" "File missing: $Path"; return $false
    }
    $content = Get-Content $Path -Raw
    $found = $content -match $Pattern
    Record-Result $Id $Name $(if ($found) { "PASS" } else { "FAIL" }) `
        $(if (-not $found) { "Pattern not found: $Pattern" } else { "" })
    return $found
}

# -----------------------------------------------------------------------
# HEADER
# -----------------------------------------------------------------------
Write-Host ""
Write-Host "========================================" -ForegroundColor Magenta
Write-Host "VIBECHECK MILESTONE 32 VERIFICATION" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta
Write-Host ""

# -----------------------------------------------------------------------
# CHECK 1: Helm lint
# -----------------------------------------------------------------------
Write-Log "STEP" "1. Helm lint"

$helmAvail = $null -ne (Get-Command helm -ErrorAction SilentlyContinue)
if (-not $helmAvail) {
    Record-Result "01" "Helm lint (default)" "SKIP" "helm not installed"
    Record-Result "01a" "Helm lint (minikube)" "SKIP" "helm not installed"
    Record-Result "01b" "Helm lint (prod)" "SKIP" "helm not installed"
} else {
    $lintDefault = helm lint k8s/helm/vibecheck 2>&1
    Record-Result "01" "Helm lint default values" `
        $(if ($LASTEXITCODE -eq 0) { "PASS" } else { "FAIL" }) `
        $(if ($LASTEXITCODE -ne 0) { $lintDefault | Select-Object -Last 3 | Out-String } else { "" })

    $lintMini = helm lint k8s/helm/vibecheck -f k8s/helm/vibecheck/values-minikube.yaml 2>&1
    Record-Result "01a" "Helm lint minikube values" `
        $(if ($LASTEXITCODE -eq 0) { "PASS" } else { "FAIL" }) ""

    $lintProd = helm lint k8s/helm/vibecheck -f k8s/helm/vibecheck/values-prod.yaml 2>&1
    Record-Result "01b" "Helm lint production values" `
        $(if ($LASTEXITCODE -eq 0) { "PASS" } else { "FAIL" }) ""
}

# -----------------------------------------------------------------------
# CHECK 2: Helm template
# -----------------------------------------------------------------------
Write-Log "STEP" "2. Helm template"

if ($helmAvail) {
    $tmpl = helm template vibecheck k8s/helm/vibecheck 2>&1
    Record-Result "02" "Helm template renders without errors" `
        $(if ($LASTEXITCODE -eq 0) { "PASS" } else { "FAIL" }) ""

    # Verify 3-broker Kafka in rendered template
    $has3Brokers = $tmpl -match "replicas:\s+3" -and $tmpl -match "kafka"
    Record-Result "02a" "Kafka StatefulSet replicas=3 in rendered template" `
        $(if ($has3Brokers) { "PASS" } else { "FAIL" }) ""
} else {
    Record-Result "02" "Helm template" "SKIP" "helm not installed"
    Record-Result "02a" "Kafka replicas in template" "SKIP" "helm not installed"
}

# -----------------------------------------------------------------------
# CHECK 3: Kubernetes manifest validation (static file check)
# -----------------------------------------------------------------------
Write-Log "STEP" "3. Kubernetes manifests"

$manifests = @(
    "k8s/helm/vibecheck/templates/infrastructure/kafka-statefulset.yaml",
    "k8s/helm/vibecheck/templates/infrastructure/kafka-service.yaml",
    "k8s/helm/vibecheck/templates/infrastructure/mysql-statefulset.yaml",
    "k8s/helm/vibecheck/templates/infrastructure/redis-statefulset.yaml",
    "k8s/helm/vibecheck/templates/infrastructure/zookeeper-deployment.yaml",
    "k8s/helm/vibecheck/templates/services/booking-service.yaml",
    "k8s/helm/vibecheck/templates/services/payment-service.yaml",
    "k8s/helm/vibecheck/templates/autoscaling/hpa.yaml",
    "k8s/helm/vibecheck/templates/autoscaling/pdb.yaml",
    "k8s/helm/vibecheck/templates/security/network-policy.yaml",
    "k8s/helm/vibecheck/templates/secrets/vibecheck-secrets.yaml",
    "k8s/helm/vibecheck/templates/secrets/external-secret.yaml",
    "k8s/helm/vibecheck/templates/secrets/cluster-secret-store.yaml"
)

$manifestsOk = $true
foreach ($m in $manifests) {
    if (-not (Test-Path $m)) {
        Record-Result "03-$(Split-Path $m -Leaf)" "Manifest exists: $(Split-Path $m -Leaf)" "FAIL" "Not found"
        $manifestsOk = $false
    }
}
if ($manifestsOk) {
    Record-Result "03" "All required Kubernetes manifests present" "PASS" "$($manifests.Count) files"
}

# -----------------------------------------------------------------------
# CHECK 4: Dockerfile non-root configuration
# -----------------------------------------------------------------------
Write-Log "STEP" "4. Dockerfile non-root configuration"

$services = @("auth-service","gateway-service","movie-service","theatre-service",
              "show-service","booking-service","payment-service","notification-service")
$nonRootCount = 0; $totalDockerfiles = 0

foreach ($svc in $services) {
    $df = "booking-system/$svc/Dockerfile"
    if (Test-Path $df) {
        $totalDockerfiles++
        $content = Get-Content $df -Raw
        if ($content -match "(?m)^USER\s+") {
            $nonRootCount++
        } else {
            Write-Log "WARN" "${svc}: No USER instruction found in Dockerfile"
        }
    }
}

if ($totalDockerfiles -eq 0) {
    Record-Result "04" "Dockerfile non-root check" "SKIP" "No Dockerfiles found at expected paths"
} elseif ($nonRootCount -eq $totalDockerfiles) {
    Record-Result "04" "All Dockerfiles have non-root USER instruction" "PASS" "$nonRootCount/$totalDockerfiles"
} elseif ($nonRootCount -gt 0) {
    Record-Result "04" "Dockerfile non-root USER instruction" "PARTIAL" "$nonRootCount/$totalDockerfiles"
} else {
    Record-Result "04" "Dockerfile non-root USER instruction" "FAIL" "0/$totalDockerfiles have USER instruction"
}

# -----------------------------------------------------------------------
# CHECK 5: Resource policies
# -----------------------------------------------------------------------
Write-Log "STEP" "5. Resource policies"

$valuesContent = Get-Content "k8s/helm/vibecheck/values.yaml" -Raw
$hasRequests = $valuesContent -match "requests:"
$hasLimits   = $valuesContent -match "limits:"
Record-Result "05" "Resource requests and limits configured" `
    $(if ($hasRequests -and $hasLimits) { "PASS" } else { "FAIL" }) ""

# -----------------------------------------------------------------------
# CHECK 6: HPA configuration
# -----------------------------------------------------------------------
Write-Log "STEP" "6. HPA configuration"

$hpaContent = Get-Content "k8s/helm/vibecheck/templates/autoscaling/hpa.yaml" -Raw
$hasHpa          = $hpaContent -match "HorizontalPodAutoscaler"
$hasStabilization = $hpaContent -match "stabilizationWindowSeconds"
$hasScaleDown     = $hpaContent -match "scaleDown"
Record-Result "06" "HPA manifest with stabilization windows" `
    $(if ($hasHpa -and $hasStabilization -and $hasScaleDown) { "PASS" } else { "FAIL" }) `
    "HPA: $hasHpa, Stabilization: $hasStabilization, ScaleDown: $hasScaleDown"

# Check HPA autoscaling.enabled for key services
$bookingHpa = $valuesContent -match "(?s)booking:.*?autoscaling:.*?enabled:\s*true"
Record-Result "06a" "booking-service HPA enabled" `
    $(if ($bookingHpa) { "PASS" } else { "FAIL" }) ""

# -----------------------------------------------------------------------
# CHECK 7: PDB configuration
# -----------------------------------------------------------------------
Write-Log "STEP" "7. PDB configuration"

$pdbContent = Get-Content "k8s/helm/vibecheck/templates/autoscaling/pdb.yaml" -Raw
$hasPdb = $pdbContent -match "PodDisruptionBudget"
Record-Result "07" "PDB manifest present" $(if ($hasPdb) { "PASS" } else { "FAIL" }) ""

$prodValues = Get-Content "k8s/helm/vibecheck/values-prod.yaml" -Raw
$pdbInProd  = $prodValues -match "pdb:.*enabled:\s*true" -or $prodValues -match "minAvailable:"
Record-Result "07a" "PDB configured for critical services in prod values" `
    $(if ($pdbInProd) { "PASS" } else { "FAIL" }) ""

# -----------------------------------------------------------------------
# CHECK 8: Network policies
# -----------------------------------------------------------------------
Write-Log "STEP" "8. Network policies"

$npContent = Get-Content "k8s/helm/vibecheck/templates/security/network-policy.yaml" -Raw
$hasNp = $npContent -match "NetworkPolicy"
$npEnabled = $valuesContent -match "(?s)networkPolicy:.*?enabled:\s*true" -or `
             $prodValues -match "(?s)networkPolicy:.*?enabled:\s*true"
Record-Result "08" "NetworkPolicy manifest present" $(if ($hasNp) { "PASS" } else { "FAIL" }) ""
Record-Result "08a" "NetworkPolicy enabled in prod values" `
    $(if ($npEnabled) { "PASS" } else { "FAIL" }) ""

# -----------------------------------------------------------------------
# CHECK 9: Secret references (no live credentials in values files)
# -----------------------------------------------------------------------
Write-Log "STEP" "9. Secret reference validation"

$valuesToCheck = @("k8s/helm/vibecheck/values.yaml", "k8s/helm/vibecheck/values-prod.yaml")
$liveCredFound = $false
foreach ($f in $valuesToCheck) {
    if (Test-Path $f) {
        $fc = Get-Content $f -Raw
        if ($fc -match "rzp_live_" -or $fc -match "sk_live_") {
            Write-Log "FAIL" "Live credentials found in $f"
            $liveCredFound = $true
        }
    }
}
Record-Result "09" "No live payment credentials in Git-tracked values files" `
    $(if (-not $liveCredFound) { "PASS" } else { "FAIL" }) `
    $(if ($liveCredFound) { "CRITICAL: Live credentials in values files" } else { "" })

# Check external secrets manifests present
$esManifest = Test-Path "k8s/helm/vibecheck/templates/secrets/external-secret.yaml"
$cssManifest = Test-Path "k8s/helm/vibecheck/templates/secrets/cluster-secret-store.yaml"
Record-Result "09a" "ExternalSecret manifest present" $(if ($esManifest) { "PASS" } else { "FAIL" }) ""
Record-Result "09b" "ClusterSecretStore manifest present" $(if ($cssManifest) { "PASS" } else { "FAIL" }) ""

# Check inline secret is conditional
$inlineSecret = Get-Content "k8s/helm/vibecheck/templates/secrets/vibecheck-secrets.yaml" -Raw
$isConditional = $inlineSecret -match "if not.*externalSecrets"
Record-Result "09c" "Inline secret is conditional on externalSecrets.enabled" `
    $(if ($isConditional) { "PASS" } else { "FAIL" }) ""

# -----------------------------------------------------------------------
# CHECK 10: Kafka HA configuration
# -----------------------------------------------------------------------
Write-Log "STEP" "10. Kafka HA configuration"

$kafkaSts = Get-Content "k8s/helm/vibecheck/templates/infrastructure/kafka-statefulset.yaml" -Raw
$hasReplicationFactor3 = $kafkaSts -match "KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR" -and
                         $valuesContent -match "replicationFactor:\s*3"
$hasMinIsr2   = $kafkaSts -match "KAFKA_MIN_INSYNC_REPLICAS" -and
                $valuesContent -match "minInSyncReplicas:\s*2"
$hasDynamicId = $kafkaSts -match "HOSTNAME.*\-.*\+"  # dynamic BROKER_ID
$has3Replicas = $valuesContent -match "replicaCount:\s*3" # Kafka replicaCount

$hasUncleanDisabled = $kafkaSts -match "(?s)KAFKA_UNCLEAN_LEADER_ELECTION_ENABLE.*?false"
$hasTxnReplication  = $kafkaSts -match "KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR"

Record-Result "10" "Kafka replication.factor >= 3 in values" `
    $(if ($hasReplicationFactor3) { "PASS" } else { "FAIL" }) ""
Record-Result "10a" "Kafka min.insync.replicas >= 2 in values" `
    $(if ($hasMinIsr2) { "PASS" } else { "FAIL" }) ""
Record-Result "10b" "Kafka dynamic BROKER_ID from pod ordinal" `
    $(if ($hasDynamicId) { "PASS" } else { "FAIL" }) ""
Record-Result "10c" "Kafka replicaCount = 3 in values" `
    $(if ($has3Replicas) { "PASS" } else { "FAIL" }) ""
Record-Result "10d" "Kafka unclean leader election disabled" `
    $(if ($hasUncleanDisabled) { "PASS" } else { "FAIL" }) ""
Record-Result "10e" "Kafka transaction log replication configured" `
    $(if ($hasTxnReplication) { "PASS" } else { "FAIL" }) ""

# Kafka headless service present
$kafkaSvcContent = Get-Content "k8s/helm/vibecheck/templates/infrastructure/kafka-service.yaml" -Raw
$hasHeadless = $kafkaSvcContent -match "clusterIP:\s*None"
Record-Result "10f" "Kafka headless service (clusterIP: None) present" `
    $(if ($hasHeadless) { "PASS" } else { "FAIL" }) ""

# Minikube override reduces to 1 broker
$minikubeValues = Get-Content "k8s/helm/vibecheck/values-minikube.yaml" -Raw
$minikubeOverride = $minikubeValues -match "replicaCount:\s*1" -and
                    $minikubeValues -match "replicationFactor:\s*1"
Record-Result "10g" "Minikube override reduces to 1 broker" `
    $(if ($minikubeOverride) { "PASS" } else { "FAIL" }) ""

# ZooKeeper is now StatefulSet
$zkContent = Get-Content "k8s/helm/vibecheck/templates/infrastructure/zookeeper-deployment.yaml" -Raw
$zkIsStatefulSet = $zkContent -match "kind:\s*StatefulSet"
$zkHasPvc        = $zkContent -match "volumeClaimTemplates"
Record-Result "10h" "ZooKeeper is StatefulSet (not Deployment)" `
    $(if ($zkIsStatefulSet) { "PASS" } else { "FAIL" }) ""
Record-Result "10i" "ZooKeeper has persistent storage (volumeClaimTemplates)" `
    $(if ($zkHasPvc) { "PASS" } else { "FAIL" }) ""

# -----------------------------------------------------------------------
# CHECK 11: Database configuration
# -----------------------------------------------------------------------
Write-Log "STEP" "11. Database configuration"

$mysqlSts = Get-Content "k8s/helm/vibecheck/templates/infrastructure/mysql-statefulset.yaml" -Raw
$mysqlHasPvc      = $mysqlSts -match "volumeClaimTemplates"
$mysqlHasReadiness = $mysqlSts -match "readinessProbe"
$mysqlHasLiveness  = $mysqlSts -match "livenessProbe"

Record-Result "11" "MySQL StatefulSet has persistent storage" `
    $(if ($mysqlHasPvc) { "PASS" } else { "FAIL" }) ""
Record-Result "11a" "MySQL has readiness + liveness probes" `
    $(if ($mysqlHasReadiness -and $mysqlHasLiveness) { "PASS" } else { "FAIL" }) ""
Record-Result "11b" "MySQL HA doc present" `
    $(if (Test-Path "docs/milestone-32-database-ha.md") { "PASS" } else { "FAIL" }) ""

# -----------------------------------------------------------------------
# CHECK 12: Redis configuration
# -----------------------------------------------------------------------
Write-Log "STEP" "12. Redis configuration"

$redisSts = Get-Content "k8s/helm/vibecheck/templates/infrastructure/redis-statefulset.yaml" -Raw
$redisAof        = $redisSts -match "appendonly"
$redisHasPvc     = $redisSts -match "volumeClaimTemplates"
$redisHasProbes  = $redisSts -match "readinessProbe" -and $redisSts -match "livenessProbe"

Record-Result "12" "Redis StatefulSet has AOF persistence" `
    $(if ($redisAof) { "PASS" } else { "FAIL" }) ""
Record-Result "12a" "Redis has persistent storage (PVC)" `
    $(if ($redisHasPvc) { "PASS" } else { "FAIL" }) ""
Record-Result "12b" "Redis has health probes" `
    $(if ($redisHasProbes) { "PASS" } else { "FAIL" }) ""

# -----------------------------------------------------------------------
# CHECK 13: Health probe configuration
# -----------------------------------------------------------------------
Write-Log "STEP" "13. Health probe configuration"

$bookingSvc = Get-Content "k8s/helm/vibecheck/templates/services/booking-service.yaml" -Raw
$hasStartup    = $bookingSvc -match "startupProbe"
$hasReadiness  = $bookingSvc -match "readinessProbe"
$hasLiveness   = $bookingSvc -match "livenessProbe"
$hasActuator   = $bookingSvc -match "/actuator/health"

Record-Result "13" "booking-service has all 3 probe types" `
    $(if ($hasStartup -and $hasReadiness -and $hasLiveness) { "PASS" } else { "FAIL" }) ""
Record-Result "13a" "Probes use /actuator/health endpoints" `
    $(if ($hasActuator) { "PASS" } else { "FAIL" }) ""

# -----------------------------------------------------------------------
# CHECK 14: Graceful shutdown
# -----------------------------------------------------------------------
Write-Log "STEP" "14. Graceful shutdown configuration"

$hasTermGrace  = $bookingSvc -match "terminationGracePeriodSeconds"
$hasPreStop    = $bookingSvc -match "preStop"
$commonConfig  = Get-Content "k8s/helm/vibecheck/templates/configmaps/common-config.yaml" -Raw

Record-Result "14" "terminationGracePeriodSeconds configured" `
    $(if ($hasTermGrace) { "PASS" } else { "FAIL" }) ""
Record-Result "14a" "preStop lifecycle hook configured" `
    $(if ($hasPreStop) { "PASS" } else { "FAIL" }) ""

# Check ROLLING UPDATE maxUnavailable=0
$hasMaxUnavailable0 = $valuesContent -match "maxUnavailable:\s*0"
Record-Result "14b" "Rolling update maxUnavailable=0 configured" `
    $(if ($hasMaxUnavailable0) { "PASS" } else { "FAIL" }) ""

# -----------------------------------------------------------------------
# CHECK 15: Observability configuration
# -----------------------------------------------------------------------
Write-Log "STEP" "15. Observability configuration"

$hasPrometheus   = Test-Path "k8s/helm/vibecheck/templates/monitoring/prometheus-configmap.yaml"
$hasGrafana      = Test-Path "k8s/helm/vibecheck/templates/monitoring/grafana-deployment.yaml"
$hasPrometheusEnv = $commonConfig -match "MANAGEMENT_METRICS_EXPORT_PROMETHEUS_ENABLED"
$hasHealthProbes  = $commonConfig -match "MANAGEMENT_ENDPOINT_HEALTH_PROBES_ENABLED"

Record-Result "15" "Prometheus ConfigMap present" $(if ($hasPrometheus) { "PASS" } else { "FAIL" }) ""
Record-Result "15a" "Grafana Deployment present" $(if ($hasGrafana) { "PASS" } else { "FAIL" }) ""
Record-Result "15b" "Prometheus metrics export enabled in ConfigMap" `
    $(if ($hasPrometheusEnv) { "PASS" } else { "FAIL" }) ""
Record-Result "15c" "Health probe endpoints enabled" `
    $(if ($hasHealthProbes) { "PASS" } else { "FAIL" }) ""

# -----------------------------------------------------------------------
# CHECK 16: Maven tests
# -----------------------------------------------------------------------
Write-Log "STEP" "16. Maven tests"

if ($SkipMaven) {
    Record-Result "16" "Maven tests" "SKIP" "-SkipMaven flag set"
} else {
    $mvnw = "booking-system/mvnw.cmd"
    if (Test-Path $mvnw) {
        Write-Log "INFO" "Running Maven tests (this may take several minutes)..."
        $mavenResult = & ".\booking-system\mvnw.cmd" test `
            --batch-mode --no-transfer-progress `
            -Dsurefire.failIfNoSpecifiedTests=false `
            -f "booking-system/pom.xml" 2>&1 |
            Tee-Object -Variable mavenOutput
        $mavenPassed = ($LASTEXITCODE -eq 0)
        Record-Result "16" "Maven unit tests" `
            $(if ($mavenPassed) { "PASS" } else { "FAIL" }) `
            $(if (-not $mavenPassed) { "Check mvn output above" } else { "All tests passed" })
    } else {
        Record-Result "16" "Maven tests" "SKIP" "mvnw.cmd not found at booking-system/mvnw.cmd"
    }
}

# -----------------------------------------------------------------------
# CHECK 17: Milestone 32 documentation completeness
# -----------------------------------------------------------------------
Write-Log "STEP" "17. Documentation completeness"

$docs = @(
    @{ Path = "docs/milestone-32-inventory.md"; Name = "M32 inventory doc" },
    @{ Path = "docs/milestone-32-infrastructure-baseline.md"; Name = "M32 baseline doc" },
    @{ Path = "docs/milestone-32-kafka-ha.md"; Name = "M32 Kafka HA doc" },
    @{ Path = "docs/milestone-32-database-ha.md"; Name = "M32 Database HA doc" },
    @{ Path = "docs/milestone-32-secret-management.md"; Name = "M32 Secret management doc" },
    @{ Path = "docs/milestone-32-disaster-recovery.md"; Name = "M32 DR runbook" },
    @{ Path = "docs/milestone-32-load-test-report.md"; Name = "M32 load test report" },
    @{ Path = "docs/milestone-32-verification.md"; Name = "M32 verification doc" },
    @{ Path = "docs/milestone-32-final-production-infrastructure-report.md"; Name = "M32 final report" }
)

$docsPresent = 0
foreach ($doc in $docs) {
    if (Test-Path $doc.Path) {
        $docsPresent++
    } else {
        Write-Log "WARN" "Missing doc: $($doc.Path)"
    }
}

if ($docsPresent -eq $docs.Count) {
    Record-Result "17" "All M32 documentation files present" "PASS" "$docsPresent/$($docs.Count)"
} elseif ($docsPresent -ge ($docs.Count - 2)) {
    Record-Result "17" "M32 documentation files" "PARTIAL" "$docsPresent/$($docs.Count)"
} else {
    Record-Result "17" "M32 documentation files" "FAIL" "$docsPresent/$($docs.Count) present"
}

# -----------------------------------------------------------------------
# CHECK 18: Test scripts present
# -----------------------------------------------------------------------
Write-Log "STEP" "18. Test scripts"

$scripts = @(
    "scripts/milestone-32-kafka-failure-test.ps1",
    "scripts/milestone-32-database-recovery-test.ps1",
    "scripts/milestone-32-secret-rotation-test.ps1",
    "scripts/verify-milestone-32.ps1"
)
$scriptsOk = $true
foreach ($s in $scripts) {
    if (-not (Test-Path $s)) { $scriptsOk = $false; Write-Log "FAIL" "Missing: $s" }
}
Record-Result "18" "All M32 test scripts present" `
    $(if ($scriptsOk) { "PASS" } else { "FAIL" }) ""

# -----------------------------------------------------------------------
# CHECK 19: CI hardening
# -----------------------------------------------------------------------
Write-Log "STEP" "19. CI hardening"

$ciContent = Get-Content ".github/workflows/ci.yml" -Raw
$hasHelmLint    = $ciContent -match "helm lint"
$hasK6Syntax    = $ciContent -match "node.*--check"
$hasNonRoot     = $ciContent -match "non-root|USER"
$hasSecretCheck = $ciContent -match "rzp_live_"
$hasInfraJob    = $ciContent -match "infrastructure-validate"

Record-Result "19" "CI has helm lint step" $(if ($hasHelmLint) { "PASS" } else { "FAIL" }) ""
Record-Result "19a" "CI has k6 syntax validation" $(if ($hasK6Syntax) { "PASS" } else { "FAIL" }) ""
Record-Result "19b" "CI has non-root Dockerfile check" $(if ($hasNonRoot) { "PASS" } else { "FAIL" }) ""
Record-Result "19c" "CI has values secret pattern check" $(if ($hasSecretCheck) { "PASS" } else { "FAIL" }) ""
Record-Result "19d" "CI has infrastructure-validate job" $(if ($hasInfraJob) { "PASS" } else { "FAIL" }) ""

# -----------------------------------------------------------------------
# FINAL REPORT
# -----------------------------------------------------------------------
$elapsed = [math]::Round(((Get-Date) - $StartTime).TotalSeconds, 1)

Write-Host ""
Write-Host "========================================" -ForegroundColor Magenta
Write-Host "VIBECHECK MILESTONE 32 VERIFICATION" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta
Write-Host ""

foreach ($r in $script:Results) {
    $color = switch ($r.Status) {
        "PASS"    { "Green"   }
        "FAIL"    { "Red"     }
        "PARTIAL" { "Yellow"  }
        "SKIP"    { "DarkGray"}
        default   { "White"   }
    }
    $icon = switch ($r.Status) {
        "PASS"    { "[PASS]   " }
        "FAIL"    { "[FAIL]   " }
        "PARTIAL" { "[PARTIAL]" }
        "SKIP"    { "[SKIP]   " }
        default   { "[???]    " }
    }
    $detail = if ($r.Detail) { "  ($($r.Detail))" } else { "" }
    Write-Host "  $icon $($r.Id.PadRight(5)) $($r.Name)$detail" -ForegroundColor $color
}

Write-Host ""
Write-Host "========================================" -ForegroundColor Magenta
Write-Host "RESULT" -ForegroundColor Magenta
Write-Host "========================================" -ForegroundColor Magenta
Write-Host "  PASS         : $($script:PassCount)" -ForegroundColor Green
Write-Host "  FAIL         : $($script:FailCount)" -ForegroundColor $(if ($script:FailCount -gt 0) { "Red" } else { "Green" })
Write-Host "  PARTIAL      : $($script:PartialCount)" -ForegroundColor Yellow
Write-Host "  SKIP         : $($script:SkipCount)" -ForegroundColor DarkGray
Write-Host "  Duration     : ${elapsed}s" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Magenta

if ($script:FailCount -gt 0) {
    Write-Host ""
    Write-Host "OVERALL: FAIL ($($script:FailCount) checks failed)" -ForegroundColor Red
    exit 1
} elseif ($script:PartialCount -gt 0) {
    Write-Host ""
    Write-Host "OVERALL: PARTIAL ($($script:PartialCount) partial checks)" -ForegroundColor Yellow
    exit 0
} else {
    Write-Host ""
    Write-Host "OVERALL: PASS" -ForegroundColor Green
    exit 0
}
