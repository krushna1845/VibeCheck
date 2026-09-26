<#
.SYNOPSIS
    VibeCheck Operations & Disaster Recovery Automated Verification Suite
    Milestone 31 — Production Operations, DR & Operational Readiness
#>

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$repoRoot = Resolve-Path (Join-Path $PSScriptRoot "..")
$passCount = 0
$failCount = 0

function Record-Result {
    param(
        [string]$TestName,
        [bool]$Passed,
        [string]$Details
    )
    if ($Passed) {
        $script:passCount++
        Write-Host ("[PASS] " + $TestName + " - " + $Details) -ForegroundColor Green
    } else {
        $script:failCount++
        Write-Host ("[FAIL] " + $TestName + " - " + $Details) -ForegroundColor Red
    }
}

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "  VibeCheck Milestone 31 - Operational Verification Suite" -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host ("Repository Root: " + $repoRoot + "`n")

# --- Test Group 1: PowerShell Script Syntax ---
Write-Host "--> Running Test Group 1: PowerShell Script Syntax..." -ForegroundColor Yellow
$scripts = Get-ChildItem -Path (Join-Path $repoRoot "scripts") -Filter "*.ps1" -Recurse
foreach ($s in $scripts) {
    $parseErrors = @()
    $tokens = $null
    [System.Management.Automation.Language.Parser]::ParseFile($s.FullName, [ref]$tokens, [ref]$parseErrors) | Out-Null
    $ok = ($parseErrors.Count -eq 0)
    $msg = "Syntax valid"
    if (-not $ok) {
        $msg = "Syntax errors: " + ($parseErrors | ForEach-Object { $_.Message } | Out-String)
    }
    Record-Result -TestName ("Script Syntax: " + $s.Name) -Passed $ok -Details $msg
}

# --- Test Group 2: Docker Compose Configuration ---
Write-Host "`n--> Running Test Group 2: Docker Compose Config..." -ForegroundColor Yellow
Push-Location $repoRoot
try {
    $dcOutput = docker compose config --quiet 2>&1
    $dcSuccess = ($LASTEXITCODE -eq 0)
    Record-Result -TestName "Docker Compose Config" -Passed $dcSuccess -Details "docker compose config valid"
} catch {
    Record-Result -TestName "Docker Compose Config" -Passed $false -Details $_.Exception.Message
} finally {
    Pop-Location
}

# --- Test Group 3: Helm Chart Validation ---
Write-Host "`n--> Running Test Group 3: Helm Chart Lint and Template..." -ForegroundColor Yellow
$chartPath = Join-Path $repoRoot "k8s\helm\vibecheck"

# Helm lint default
$lintOutput = helm lint $chartPath 2>&1
Record-Result -TestName "Helm Lint (Default)" -Passed ($LASTEXITCODE -eq 0) -Details "0 errors"

# Helm lint prod
$prodValues = Join-Path $chartPath "values-prod.yaml"
$lintProd = helm lint $chartPath -f $prodValues 2>&1
Record-Result -TestName "Helm Lint (Prod Values)" -Passed ($LASTEXITCODE -eq 0) -Details "0 errors"

# Helm template default
$tmplOutput = helm template vibecheck $chartPath 2>&1
$tmplSuccess = ($LASTEXITCODE -eq 0) -and ($tmplOutput -match "kind: Deployment")
Record-Result -TestName "Helm Template Render (Default)" -Passed $tmplSuccess -Details "Deployments rendered"

# Helm template prod (with PDBs)
$tmplProd = helm template vibecheck $chartPath -f $prodValues 2>&1
$pdbFound = ($LASTEXITCODE -eq 0) -and ($tmplProd -match "kind: PodDisruptionBudget")
Record-Result -TestName "Helm Template Render (Prod with PDB)" -Passed $pdbFound -Details "PodDisruptionBudgets rendered"

# --- Test Group 4: Backup Integrity Verifier Negative Tests ---
Write-Host "`n--> Running Test Group 4: Backup Verifier Boundary Checks..." -ForegroundColor Yellow
$verifyScript = Join-Path $repoRoot "scripts\backup\verify-backup.ps1"
$psExe = if (Get-Command pwsh -ErrorAction SilentlyContinue) { "pwsh" } else { "powershell" }

# Negative Test: non-existent file
$negOut1 = & $psExe -NoProfile -File $verifyScript -BackupPath "non_existent_file_test.sql.gz" 2>&1
Record-Result -TestName "Verify Script: Non-existent file rejection" -Passed ($LASTEXITCODE -ne 0) -Details "Correctly rejected non-existent file"

# Negative Test: empty file
$tempEmpty = [System.IO.Path]::GetTempFileName()
$negOut2 = & $psExe -NoProfile -File $verifyScript -BackupPath $tempEmpty 2>&1
Record-Result -TestName "Verify Script: Zero-byte file rejection" -Passed ($LASTEXITCODE -ne 0) -Details "Correctly rejected empty file"
if (Test-Path $tempEmpty) { Remove-Item -Force $tempEmpty }

# --- Test Group 5: Graceful Shutdown Configuration Audit ---
Write-Host "`n--> Running Test Group 5: Spring Boot Graceful Shutdown Config..." -ForegroundColor Yellow
$serviceDirs = @("auth-service", "movie-service", "theatre-service", "show-service", "booking-service", "payment-service", "notification-service", "gateway-service")
foreach ($svc in $serviceDirs) {
    $appYml = Join-Path $repoRoot ("booking-system\" + $svc + "\src\main\resources\application.yml")
    if (Test-Path $appYml) {
        $content = Get-Content $appYml -Raw
        $hasShutdown = $content -match "shutdown:\s*graceful"
        $hasTimeout = $content -match "timeout-per-shutdown-phase:\s*30s"
        $configured = $hasShutdown -and $hasTimeout
        Record-Result -TestName ("Graceful Shutdown: " + $svc) -Passed $configured -Details "server.shutdown: graceful and timeout-per-shutdown-phase: 30s verified"
    } else {
        Record-Result -TestName ("Graceful Shutdown: " + $svc) -Passed $false -Details "application.yml not found"
    }
}

# --- Summary ---
Write-Host "`n=================================================================" -ForegroundColor Cyan
Write-Host "  VERIFICATION SUMMARY" -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host ("Total Checks : " + ($passCount + $failCount))
Write-Host ("Passed       : " + $passCount) -ForegroundColor Green
Write-Host ("Failed       : " + $failCount) -ForegroundColor $(if ($failCount -gt 0) { "Red" } else { "Green" })

if ($failCount -gt 0) {
    Write-Host "`nOVERALL STATUS: FAILED" -ForegroundColor Red
    exit 1
} else {
    Write-Host "`nOVERALL STATUS: ALL CHECKS PASSED" -ForegroundColor Green
    exit 0
}
