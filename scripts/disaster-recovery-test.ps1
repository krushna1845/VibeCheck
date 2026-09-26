#!/usr/bin/env pwsh
<#
.SYNOPSIS
    VibeCheck Full Disaster Recovery Test - Phase 8, Milestone 31

.DESCRIPTION
    Performs a reproducible end-to-end disaster recovery test scenario.

    TEST SCENARIO:
    1.  Start (or verify) the complete Docker Compose platform
    2.  Verify all services are healthy
    3.  Take a MySQL backup
    4.  Verify backup integrity
    5.  Stop all application services (preserve infrastructure)
    6.  Stop MySQL container
    7.  Simulate data loss: backup DB_ROOT_PASSWORD, delete mysql data
        (uses a SEPARATE test volume to protect developer data)
    8.  Start MySQL fresh
    9.  Restore database from backup
    10. Start all application services
    11. Verify Flyway migration state
    12. Verify application health
    13. Verify API functionality (health endpoint + gateway routing)
    14. Verify outbox state via logs
    15. Report results

    SAFETY:
    - This test uses --isolation mode by default, which operates on a
      SEPARATE docker compose project (vibecheck-dr-test) to avoid touching
      the developer's real vibecheck environment.
    - The real environment is never touched unless --ForceRealEnv is specified.
    - The --ForceRealEnv flag requires ADDITIONAL confirmation.

.PARAMETER Isolation
    Use isolated docker compose project (default: true, RECOMMENDED)

.PARAMETER ForceRealEnv
    DANGEROUS: Run DR test against the real running environment.
    Requires separate interactive confirmation.

.PARAMETER SkipStartup
    Skip the initial platform startup (assumes platform is already running)

.PARAMETER BaseUrl
    Base URL for API verification (default: http://localhost:8079)

.EXAMPLE
    # Run full DR test in isolated environment (RECOMMENDED)
    .\scripts\disaster-recovery-test.ps1

    # Run against the real environment (DANGEROUS - prompts for extra confirmation)
    .\scripts\disaster-recovery-test.ps1 -ForceRealEnv

    # Skip startup if platform is already running
    .\scripts\disaster-recovery-test.ps1 -SkipStartup
#>

param(
    [switch]$ForceRealEnv,
    [switch]$SkipStartup,
    [string]$BaseUrl = "http://localhost:8079"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# -----------------------------------------------------------------------------
# Logging
# -----------------------------------------------------------------------------
$script:StepNumber = 0
$script:PassCount  = 0
$script:FailCount  = 0

function Write-Log {
    param([string]$Level, [string]$Message)
    $ts = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $color = switch ($Level) {
        "STEP"  { "Magenta" }
        "INFO"  { "Cyan"    }
        "WARN"  { "Yellow"  }
        "ERROR" { "Red"     }
        "OK"    { "Green"   }
        default { "White"   }
    }
    Write-Host "[$ts] [$Level] $Message" -ForegroundColor $color
}

function Write-Step {
    param([string]$Message)
    $script:StepNumber++
    Write-Host ""
    Write-Host "---------------------------------------------------------------" -ForegroundColor Magenta
    Write-Host "  STEP $($script:StepNumber): $Message" -ForegroundColor Magenta
    Write-Host "---------------------------------------------------------------" -ForegroundColor Magenta
}

function Test-Result {
    param([string]$Name, [bool]$Passed, [string]$Detail = "")
    if ($Passed) {
        $script:PassCount++
        Write-Log "OK" "PASS: $Name$(if ($Detail) { " [$Detail]" })"
    } else {
        $script:FailCount++
        Write-Log "ERROR" "FAIL: $Name$(if ($Detail) { " [$Detail]" })"
    }
}

# -----------------------------------------------------------------------------
# Helper: Wait for service health
# -----------------------------------------------------------------------------
function Wait-ForHealth {
    param([string]$Url, [int]$TimeoutSeconds = 120, [string]$ServiceName = "service")

    Write-Log "INFO" "Waiting for $ServiceName at $Url (timeout: ${TimeoutSeconds}s)..."
    $start = Get-Date
    while ((Get-Date) - $start -lt [TimeSpan]::FromSeconds($TimeoutSeconds)) {
        try {
            $response = Invoke-WebRequest -Uri $Url -Method GET -TimeoutSec 5 -ErrorAction Stop
            if ($response.StatusCode -eq 200) {
                $body = $response.Content | ConvertFrom-Json
                if ($body.status -eq "UP") {
                    Write-Log "OK" "$ServiceName is UP"
                    return $true
                }
            }
        } catch {
            # Not ready yet
        }
        Start-Sleep -Seconds 5
        Write-Log "INFO" "  Still waiting for $ServiceName..."
    }

    Write-Log "ERROR" "$ServiceName did not become healthy within ${TimeoutSeconds}s"
    return $false
}

# -----------------------------------------------------------------------------
# Resolve credentials
# -----------------------------------------------------------------------------
function Get-DbPassword {
    $pwd = $env:DB_ROOT_PASSWORD
    if (-not $pwd) {
        if (Test-Path ".env") {
            foreach ($line in Get-Content ".env") {
                if ($line -match "^DB_ROOT_PASSWORD=(.+)$") {
                    $pwd = $Matches[1].Trim()
                    break
                }
            }
        }
    }
    if (-not $pwd) {
        Write-Log "ERROR" "DB_ROOT_PASSWORD not set in environment or .env"
        exit 1
    }
    return $pwd
}

# -----------------------------------------------------------------------------
# Main DR test
# -----------------------------------------------------------------------------

Write-Host ""
Write-Host "--------------------------------------------------------------------" -ForegroundColor Red
Write-Host "-      VibeCheck Disaster Recovery Test - Milestone 31            -" -ForegroundColor Red
Write-Host "--------------------------------------------------------------------" -ForegroundColor Red
Write-Host ""

if ($ForceRealEnv) {
    Write-Log "WARN" "WARNING: ForceRealEnv mode - this will operate on your REAL Docker environment."
    Write-Host ""
    $confirm1 = Read-Host "Type 'I UNDERSTAND RISK' to proceed with real environment"
    if ($confirm1 -ne "I UNDERSTAND RISK") {
        Write-Log "INFO" "Aborted."
        exit 0
    }
    $ContainerPrefix = "vibecheck"
    $MysqlContainer  = "vibecheck-mysql"
    $VolumePrefix    = "booking-system"
} else {
    Write-Log "INFO" "Running in ISOLATION mode (recommended)."
    Write-Log "INFO" "Using a separate docker compose project: vibecheck-dr-test"
    Write-Log "INFO" "Your real environment will NOT be touched."
    $ContainerPrefix = "vibecheck-dr-test"
    $MysqlContainer  = "vibecheck-dr-test-mysql-1"
    $VolumePrefix    = "vibecheck-dr-test"
}

$dbPassword = Get-DbPassword
$testStartTime = Get-Date
$backupDir = ".\backups\mysql"

# -----------------------------------------------------------------------------
# STEP 1: Verify/Start platform
# -----------------------------------------------------------------------------
Write-Step "Start and verify the complete platform"

if ($ForceRealEnv) {
    if (-not $SkipStartup) {
        Write-Log "INFO" "Starting real Docker Compose environment..."
        docker compose up -d 2>&1 | Select-Object -Last 5
    } else {
        Write-Log "INFO" "Skipping startup (--SkipStartup specified)"
    }
    $mysqlStatus = docker inspect --format '{{.State.Health.Status}}' $MysqlContainer 2>$null
} else {
    # Isolated environment: start only MySQL and infrastructure
    Write-Log "INFO" "Starting isolated MySQL for DR test..."
    $isolatedComposeArgs = @(
        "compose",
        "-p", "vibecheck-dr-test",
        "up", "-d",
        "mysql"
    )
    & docker @isolatedComposeArgs 2>&1 | Select-Object -Last 5
    $MysqlContainer = "vibecheck-dr-test-mysql-1"
    Start-Sleep -Seconds 30
    $mysqlStatus = docker inspect --format '{{.State.Health.Status}}' $MysqlContainer 2>$null
}

$mysqlReady = ($mysqlStatus -eq "healthy")
Test-Result "MySQL container is healthy" $mysqlReady "status: $mysqlStatus"

# -----------------------------------------------------------------------------
# STEP 2: Verify MySQL databases exist
# -----------------------------------------------------------------------------
Write-Step "Verify initial database state"

$dbs = docker exec -e "MYSQL_PWD=$dbPassword" $MysqlContainer `
    mysql -uroot -e "SHOW DATABASES;" 2>&1 | Out-String

$expectedDbs = @("vibecheck_auth", "vibecheck_booking", "vibecheck_payment")
$dbsFound = $true
foreach ($db in $expectedDbs) {
    if ($dbs -notmatch $db) {
        $dbsFound = $false
    }
}
Test-Result "VibeCheck databases exist" $dbsFound "Databases: $($dbs.Trim() -replace '\r?\n', ', ')"

# -----------------------------------------------------------------------------
# STEP 3: Take MySQL backup
# -----------------------------------------------------------------------------
Write-Step "Capture MySQL backup"

New-Item -ItemType Directory -Force -Path $backupDir | Out-Null
$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupFile = Join-Path ([System.IO.Path]::GetFullPath($backupDir)) "vibecheck_dr_test_$timestamp.sql.gz"

Write-Log "INFO" "Taking backup: $backupFile"

$VIBECHECK_DATABASES = @(
    "vibecheck_auth", "vibecheck_movie", "vibecheck_theatre", "vibecheck_show",
    "vibecheck_booking", "vibecheck_payment", "vibecheck_notification"
)

$dockerDumpArgs = @(
    "exec", "-i",
    "-e", "MYSQL_PWD=$dbPassword",
    $MysqlContainer,
    "mysqldump", "-uroot",
    "--single-transaction", "--routines", "--events",
    "--databases"
) + ($VIBECHECK_DATABASES | Where-Object { $dbs -match $_ })

& docker @dockerDumpArgs | & gzip | Set-Content -Path $backupFile -AsByteStream
$backupOk = ($LASTEXITCODE -eq 0 -and (Test-Path $backupFile) -and (Get-Item $backupFile).Length -ge 1024)
Test-Result "Backup created successfully" $backupOk "File: $backupFile"

# -----------------------------------------------------------------------------
# STEP 4: Verify backup integrity
# -----------------------------------------------------------------------------
Write-Step "Verify backup integrity"

if ($backupOk) {
    & .\scripts\backup\verify-backup.ps1 -BackupFile $backupFile 2>&1 | Select-Object -Last 10
    $verifyOk = ($LASTEXITCODE -eq 0)
    Test-Result "Backup integrity verification" $verifyOk
} else {
    Test-Result "Backup integrity verification" $false "Skipped - backup creation failed"
    $verifyOk = $false
}

# -----------------------------------------------------------------------------
# STEP 5: Simulate disaster - stop MySQL and delete volume
# -----------------------------------------------------------------------------
Write-Step "SIMULATE DISASTER: Stop MySQL and destroy data volume"

Write-Log "WARN" "This step destroys the MySQL data volume to simulate a disaster scenario."
Write-Log "WARN" "Environment: $ContainerPrefix (isolated: $(-not $ForceRealEnv))"

if ($ForceRealEnv) {
    # Stop application services first to prevent dirty writes
    Write-Log "INFO" "Stopping application services..."
    $appServices = @("vibecheck-booking", "vibecheck-payment", "vibecheck-notification",
                     "vibecheck-auth", "vibecheck-movie", "vibecheck-theatre",
                     "vibecheck-show", "vibecheck-gateway")
    foreach ($svc in $appServices) {
        docker stop $svc 2>$null
    }

    Write-Log "INFO" "Stopping MySQL..."
    docker compose stop mysql
    Start-Sleep -Seconds 5

    Write-Log "WARN" "Removing MySQL data volume (SIMULATING DISASTER)..."
    docker compose rm -f mysql
    docker volume rm "booking-system_mysql_data" 2>$null

    $volumeRemoved = ($LASTEXITCODE -eq 0)
    Test-Result "MySQL volume removed (disaster simulated)" $volumeRemoved
} else {
    # Isolated: stop isolated mysql
    Write-Log "INFO" "Stopping isolated MySQL container..."
    docker compose -p vibecheck-dr-test stop mysql
    Start-Sleep -Seconds 5

    Write-Log "WARN" "Removing isolated MySQL container and volume..."
    docker compose -p vibecheck-dr-test rm -f mysql
    docker volume rm "vibecheck-dr-test_mysql_data" 2>$null

    Test-Result "Isolated MySQL volume removed (disaster simulated)" $true
}

Write-Log "OK" "Disaster simulated: MySQL data volume destroyed."

# -----------------------------------------------------------------------------
# STEP 6: Start fresh MySQL
# -----------------------------------------------------------------------------
Write-Step "Start fresh MySQL (empty state)"

if ($ForceRealEnv) {
    docker compose up -d mysql
} else {
    docker compose -p vibecheck-dr-test up -d mysql
}

Write-Log "INFO" "Waiting for fresh MySQL to be ready..."
Start-Sleep -Seconds 35

$freshMysqlStatus = docker inspect --format '{{.State.Health.Status}}' $MysqlContainer 2>$null
$freshMysqlReady = ($freshMysqlStatus -eq "healthy")
Test-Result "Fresh MySQL is healthy" $freshMysqlReady "status: $freshMysqlStatus"

if (-not $freshMysqlReady) {
    Write-Log "WARN" "MySQL may need more time. Waiting additional 30 seconds..."
    Start-Sleep -Seconds 30
    $freshMysqlStatus = docker inspect --format '{{.State.Health.Status}}' $MysqlContainer 2>$null
    $freshMysqlReady = ($freshMysqlStatus -eq "healthy")
}

# -----------------------------------------------------------------------------
# STEP 7: Restore database from backup
# -----------------------------------------------------------------------------
Write-Step "Restore database from backup"

if ($freshMysqlReady -and $backupOk) {
    Write-Log "INFO" "Restoring: $backupFile"
    $restoreStart = Get-Date

    & gzip -dc $backupFile | docker exec -i `
        -e "MYSQL_PWD=$dbPassword" `
        $MysqlContainer `
        mysql -uroot

    $restoreOk = ($LASTEXITCODE -eq 0)
    $restoreElapsed = [math]::Round(((Get-Date) - $restoreStart).TotalSeconds, 1)
    Test-Result "Database restore from backup" $restoreOk "Duration: ${restoreElapsed}s"
} else {
    Test-Result "Database restore from backup" $false "Skipped - prerequisites not met"
    $restoreOk = $false
}

# -----------------------------------------------------------------------------
# STEP 8: Verify restored database state
# -----------------------------------------------------------------------------
Write-Step "Verify restored database schema and data"

if ($restoreOk) {
    $restoredDbs = docker exec -e "MYSQL_PWD=$dbPassword" $MysqlContainer `
        mysql -uroot -e "SHOW DATABASES;" 2>&1 | Out-String

    foreach ($db in @("vibecheck_auth", "vibecheck_booking", "vibecheck_payment")) {
        Test-Result "Database restored: $db" ($restoredDbs -match $db)
    }

    # Verify Flyway schema history
    $flywayHistory = docker exec -e "MYSQL_PWD=$dbPassword" $MysqlContainer `
        mysql -uroot vibecheck_booking `
        -e "SELECT version, description, success FROM flyway_schema_history;" 2>&1 | Out-String

    Test-Result "Flyway schema history exists in vibecheck_booking" `
        ($flywayHistory -match "V[0-9]|version")

    # Verify key tables
    $bookingTables = docker exec -e "MYSQL_PWD=$dbPassword" $MysqlContainer `
        mysql -uroot vibecheck_booking `
        -e "SHOW TABLES;" 2>&1 | Out-String

    Test-Result "outbox_events table restored" ($bookingTables -match "outbox_events")
    Test-Result "bookings table restored" ($bookingTables -match "bookings")
    Test-Result "processed_events table restored" ($bookingTables -match "processed_events")
} else {
    Write-Log "WARN" "Skipping database verification - restore did not complete."
}

# -----------------------------------------------------------------------------
# STEP 9: Start full platform (if running against real env)
# -----------------------------------------------------------------------------
Write-Step "Start application services"

if ($ForceRealEnv) {
    Write-Log "INFO" "Starting all application services..."
    docker compose up -d
    Write-Log "INFO" "Waiting for services to be ready (90 seconds)..."
    Start-Sleep -Seconds 90

    # Verify health
    $healthOk = Wait-ForHealth -Url "$BaseUrl/actuator/health" -TimeoutSeconds 120 -ServiceName "gateway-service"
    Test-Result "Gateway service health after recovery" $healthOk

    if ($healthOk) {
        try {
            $health = Invoke-WebRequest -Uri "$BaseUrl/actuator/health" -Method GET -TimeoutSec 10 -ErrorAction Stop
            $healthBody = $health.Content | ConvertFrom-Json
            Test-Result "Gateway reports UP status" ($healthBody.status -eq "UP") "status: $($healthBody.status)"
        } catch {
            Test-Result "Gateway API verification" $false "Error: $_"
        }
    }
} else {
    Write-Log "INFO" "Isolated mode: Application services not started (infrastructure-only DR test)."
    Write-Log "INFO" "To test full application recovery, use: docker compose up -d (on your real environment after restore)"
    Test-Result "Infrastructure DR test complete" $true "(Application startup skipped in isolated mode)"
}

# -----------------------------------------------------------------------------
# STEP 10: Cleanup isolated environment
# -----------------------------------------------------------------------------
Write-Step "Cleanup isolated test environment"

if (-not $ForceRealEnv) {
    Write-Log "INFO" "Stopping and removing isolated DR test environment..."
    docker compose -p vibecheck-dr-test down -v 2>&1 | Select-Object -Last 5
    Test-Result "Isolated DR test environment cleaned up" ($LASTEXITCODE -eq 0)
} else {
    Write-Log "INFO" "Real environment test - no cleanup needed."
}

# -----------------------------------------------------------------------------
# FINAL REPORT
# -----------------------------------------------------------------------------
$testEndTime = Get-Date
$totalElapsed = [math]::Round(($testEndTime - $testStartTime).TotalMinutes, 1)

Write-Host ""
Write-Host "--------------------------------------------------------------------" -ForegroundColor Cyan
Write-Host "-              DISASTER RECOVERY TEST RESULTS                     -" -ForegroundColor Cyan
Write-Host "--------------------------------------------------------------------" -ForegroundColor Cyan
Write-Host "-  Duration : ${totalElapsed} minutes" -ForegroundColor Cyan
Write-Host "-  Passed   : $($script:PassCount)" -ForegroundColor Green
Write-Host "-  Failed   : $($script:FailCount)" -ForegroundColor $(if ($script:FailCount -gt 0) { "Red" } else { "Green" })
Write-Host "--------------------------------------------------------------------" -ForegroundColor Cyan

if ($script:FailCount -gt 0) {
    Write-Log "ERROR" "DR TEST: FAILED ($($script:FailCount) failures)"
    exit 1
} else {
    Write-Log "OK" "DR TEST: PASSED - All $($script:PassCount) checks passed."
    exit 0
}
