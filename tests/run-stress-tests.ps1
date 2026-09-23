# ==============================================================================
# VibeCheck High-Concurrency Stress Test & Autoscaling Runner
# ==============================================================================
param(
    [string]$GatewayUrl = "http://127.0.0.1:8079",
    [int]$Concurrency = 200
)

$ErrorActionPreference = "Continue"

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host " VIBECHECK CONCURRENCY BENCHMARK & HPA VERIFICATION" -ForegroundColor Cyan
Write-Host " Gateway URL: $GatewayUrl" -ForegroundColor Cyan
Write-Host " Concurrency: $Concurrency concurrent users" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

# 1. Inspect Horizontal Pod Autoscaler (HPA) in Minikube if cluster is active
Write-Host "`n[Step 1] Inspecting Kubernetes Horizontal Pod Autoscaler (HPA)..." -ForegroundColor Yellow
try {
    kubectl get hpa -n vibecheck -o wide
    Write-Host "`n[Step 1.1] Inspecting Current Pod Resource Consumption..." -ForegroundColor Yellow
    kubectl top pods -n vibecheck
} catch {
    Write-Host "Kubernetes cluster not accessible locally or kubectl not connected. Proceeding with benchmark..." -ForegroundColor Gray
}

# 2. Execute High-Concurrency Benchmark
Write-Host "`n[Step 2] Executing Concurrency Benchmark (200 concurrent users racing for 1 seat)..." -ForegroundColor Yellow
$env:GATEWAY_URL = $GatewayUrl
$env:CONCURRENCY = $Concurrency.ToString()

node "$PSScriptRoot\concurrency-benchmark.js"

# 3. Post-Test HPA & Pod Inspection
Write-Host "`n[Step 3] Post-Benchmark Autoscaling Status..." -ForegroundColor Yellow
try {
    kubectl get hpa -n vibecheck
} catch {
    Write-Host "HPA verification skipped (cluster offline)." -ForegroundColor Gray
}

Write-Host "`n========================================================" -ForegroundColor Green
Write-Host " BENCHMARK COMPLETE! Results saved to tests\benchmark_results.md" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Green
