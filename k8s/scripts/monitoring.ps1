#!/usr/bin/env pwsh
# ==============================================================================
# VibeCheck Monitoring Port-Forward Script
# Milestone 25: Prometheus + Grafana
#
# Usage: .\k8s\scripts\monitoring.ps1
# ==============================================================================

$NAMESPACE = "vibecheck"
$PROMETHEUS_LOCAL_PORT = 9090
$GRAFANA_LOCAL_PORT = 3000

Write-Host ""
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host "  VibeCheck Observability Stack — Port Forwarder" -ForegroundColor Cyan
Write-Host "======================================================" -ForegroundColor Cyan
Write-Host ""

# --- Wait for Prometheus pod to be ready ---
Write-Host "[1/3] Waiting for Prometheus pod to be Ready..." -ForegroundColor Yellow
$retries = 0
do {
    $promReady = kubectl get pods -n $NAMESPACE -l app.kubernetes.io/name=prometheus --no-headers 2>$null |
        Where-Object { $_ -match "Running" -and $_ -match "1/1" }
    if (-not $promReady) {
        $retries++
        if ($retries -gt 30) {
            Write-Host "[ERROR] Prometheus pod did not become ready within 5 minutes." -ForegroundColor Red
            Write-Host "        Run: kubectl get pods -n $NAMESPACE -l app.kubernetes.io/name=prometheus" -ForegroundColor Gray
            exit 1
        }
        Start-Sleep -Seconds 10
        Write-Host "        ...still waiting ($($retries * 10)s elapsed)" -ForegroundColor Gray
    }
} while (-not $promReady)
Write-Host "  [OK] Prometheus is Running!" -ForegroundColor Green

# --- Wait for Grafana pod to be ready ---
Write-Host "[2/3] Waiting for Grafana pod to be Ready..." -ForegroundColor Yellow
$retries = 0
do {
    $grafanaReady = kubectl get pods -n $NAMESPACE -l app.kubernetes.io/name=grafana --no-headers 2>$null |
        Where-Object { $_ -match "Running" -and $_ -match "1/1" }
    if (-not $grafanaReady) {
        $retries++
        if ($retries -gt 30) {
            Write-Host "[ERROR] Grafana pod did not become ready within 5 minutes." -ForegroundColor Red
            Write-Host "        Run: kubectl get pods -n $NAMESPACE -l app.kubernetes.io/name=grafana" -ForegroundColor Gray
            exit 1
        }
        Start-Sleep -Seconds 10
        Write-Host "        ...still waiting ($($retries * 10)s elapsed)" -ForegroundColor Gray
    }
} while (-not $grafanaReady)
Write-Host "  [OK] Grafana is Running!" -ForegroundColor Green

# --- Start port-forwards in background jobs ---
Write-Host "[3/3] Starting port-forward tunnels..." -ForegroundColor Yellow

$promJob = Start-Job -ScriptBlock {
    param($ns, $port)
    kubectl port-forward svc/prometheus ${port}:9090 -n $ns 2>&1
} -ArgumentList $NAMESPACE, $PROMETHEUS_LOCAL_PORT

$grafanaJob = Start-Job -ScriptBlock {
    param($ns, $port)
    kubectl port-forward svc/grafana ${port}:3000 -n $ns 2>&1
} -ArgumentList $NAMESPACE, $GRAFANA_LOCAL_PORT

Start-Sleep -Seconds 3

# --- Print Access Info ---
Write-Host ""
Write-Host "======================================================" -ForegroundColor Green
Write-Host "  Monitoring Stack is LIVE!" -ForegroundColor Green
Write-Host "======================================================" -ForegroundColor Green
Write-Host ""
Write-Host "  Prometheus:" -ForegroundColor Cyan
Write-Host "    URL    : http://localhost:$PROMETHEUS_LOCAL_PORT" -ForegroundColor White
Write-Host "    Targets: http://localhost:$PROMETHEUS_LOCAL_PORT/targets" -ForegroundColor White
Write-Host ""
Write-Host "  Grafana:" -ForegroundColor Cyan
Write-Host "    URL      : http://localhost:$GRAFANA_LOCAL_PORT" -ForegroundColor White
Write-Host "    Username : admin" -ForegroundColor White
Write-Host "    Password : vibecheck" -ForegroundColor White
Write-Host "    Dashboard: VibeCheck > VibeCheck — Service Overview" -ForegroundColor White
Write-Host ""
Write-Host "  Press CTRL+C to stop port-forwarding." -ForegroundColor Yellow
Write-Host ""

# --- Keep alive until user exits ---
try {
    while ($true) {
        # Check jobs are still alive
        $promState  = (Get-Job -Id $promJob.Id).State
        $grafState  = (Get-Job -Id $grafanaJob.Id).State
        if ($promState -eq "Failed") {
            Write-Host "[WARN] Prometheus port-forward died, restarting..." -ForegroundColor Yellow
            Stop-Job  $promJob -ErrorAction SilentlyContinue
            Remove-Job $promJob -ErrorAction SilentlyContinue
            $promJob = Start-Job -ScriptBlock {
                param($ns, $port)
                kubectl port-forward svc/prometheus ${port}:9090 -n $ns 2>&1
            } -ArgumentList $NAMESPACE, $PROMETHEUS_LOCAL_PORT
        }
        if ($grafState -eq "Failed") {
            Write-Host "[WARN] Grafana port-forward died, restarting..." -ForegroundColor Yellow
            Stop-Job  $grafanaJob -ErrorAction SilentlyContinue
            Remove-Job $grafanaJob -ErrorAction SilentlyContinue
            $grafanaJob = Start-Job -ScriptBlock {
                param($ns, $port)
                kubectl port-forward svc/grafana ${port}:3000 -n $ns 2>&1
            } -ArgumentList $NAMESPACE, $GRAFANA_LOCAL_PORT
        }
        Start-Sleep -Seconds 15
    }
}
finally {
    Write-Host ""
    Write-Host "Stopping port-forward tunnels..." -ForegroundColor Yellow
    Stop-Job  $promJob    -ErrorAction SilentlyContinue
    Stop-Job  $grafanaJob -ErrorAction SilentlyContinue
    Remove-Job $promJob    -ErrorAction SilentlyContinue
    Remove-Job $grafanaJob -ErrorAction SilentlyContinue
    Write-Host "Done. Bye!" -ForegroundColor Green
}
