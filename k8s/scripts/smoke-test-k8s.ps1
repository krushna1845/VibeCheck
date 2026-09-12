# ==============================================================================
# VibeCheck Platform - Kubernetes Cluster Smoke Test
# ==============================================================================
param(
    [string]$BaseUrl = "http://localhost:8079"
)

$ErrorActionPreference = "Stop"

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host " VIBECHECK KUBERNETES END-TO-END SMOKE TEST" -ForegroundColor Cyan
Write-Host " Target URL: $BaseUrl" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

# 1. Gateway Health Check
Write-Host "`n[Test 1] Testing Gateway Health Endpoint..." -ForegroundColor Yellow
try {
    $health = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -Method Get -TimeoutSec 10
    if ($health.status -eq "UP") {
        Write-Host "PASS: Gateway reports UP status." -ForegroundColor Green
    } else {
        Write-Host "FAIL: Gateway status: $($health.status)" -ForegroundColor Red
        exit 1
    }
} catch {
    Write-Host "FAIL: Unable to reach Gateway at $BaseUrl/actuator/health" -ForegroundColor Red
    Write-Host "Hint: Ensure port-forwarding is running: kubectl port-forward svc/gateway-service 8079:8079 -n vibecheck" -ForegroundColor Yellow
    exit 1
}

# 2. User Registration
$timestamp = Get-Date -Format "yyyyMMddHHmmss"
$testEmail = "k8s_user_${timestamp}@vibecheck.com"
$testPassword = "Password@123"

Write-Host "`n[Test 2] Testing User Registration ($testEmail)..." -ForegroundColor Yellow
$regBody = @{
    fullName = "Kubernetes Smoke Test"
    email = $testEmail
    password = $testPassword
    phoneNumber = "+1555000$([math]::Abs($timestamp.GetHashCode()) % 10000)"
} | ConvertTo-Json

try {
    $regResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/register" -Method Post `
        -ContentType "application/json" -Body $regBody -TimeoutSec 15
    Write-Host "PASS: Registered user with ID: $($regResponse.data.id)" -ForegroundColor Green
} catch {
    Write-Host "FAIL: User registration failed: $_" -ForegroundColor Red
    exit 1
}

# 3. User Authentication (JWT acquisition)
Write-Host "`n[Test 3] Testing User Authentication & Token Acquisition..." -ForegroundColor Yellow
$loginBody = @{
    email = $testEmail
    password = $testPassword
} | ConvertTo-Json

try {
    $loginResponse = Invoke-RestMethod -Uri "$BaseUrl/auth/login" -Method Post `
        -ContentType "application/json" -Body $loginBody -TimeoutSec 15
    $jwtToken = $loginResponse.data.token
    if (-not $jwtToken) {
        $jwtToken = $loginResponse.data.accessToken
    }
    Write-Host "PASS: Successfully acquired JWT token: $($jwtToken.Substring(0, 20))..." -ForegroundColor Green
} catch {
    Write-Host "FAIL: Login failed: $_" -ForegroundColor Red
    exit 1
}

# 4. Authenticated Request via Gateway to Downstream Services
Write-Host "`n[Test 4] Testing Gateway JWT Routing to Protected Downstream Services..." -ForegroundColor Yellow
$headers = @{
    "Authorization" = "Bearer $jwtToken"
}

try {
    $moviesResponse = Invoke-RestMethod -Uri "$BaseUrl/movies" -Method Get -Headers $headers -TimeoutSec 15
    Write-Host "PASS: Successfully routed authenticated request to movie-service via Gateway!" -ForegroundColor Green
} catch {
    Write-Host "FAIL: Movie-service call failed: $_" -ForegroundColor Red
    exit 1
}

# 5. Cluster Pod Status Summary
Write-Host "`n[Test 5] Checking Pod Status in vibecheck namespace..." -ForegroundColor Yellow
$pods = kubectl get pods -n vibecheck -o json | ConvertFrom-Json
$unhealthyPods = @()

foreach ($pod in $pods.items) {
    $podName = $pod.metadata.name
    $phase = $pod.status.phase
    $readyConditions = $pod.status.conditions | Where-Object { $_.type -eq "Ready" }
    $isReady = $readyConditions.status -eq "True"

    if ($phase -ne "Running" -or -not $isReady) {
        $unhealthyPods += "$podName (Phase: $phase, Ready: $isReady)"
    }
}

if ($unhealthyPods.Count -eq 0) {
    Write-Host "PASS: All $($pods.items.Count) pods in vibecheck namespace are Running and Ready!" -ForegroundColor Green
} else {
    Write-Host "WARN: Some pods are not ready yet:" -ForegroundColor Yellow
    $unhealthyPods | ForEach-Object { Write-Host "  - $_" -ForegroundColor Yellow }
}

Write-Host "`n========================================================" -ForegroundColor Green
Write-Host " ALL KUBERNETES SMOKE TESTS PASSED SUCCESSFULLY!" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Green
