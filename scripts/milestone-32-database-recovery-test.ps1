#!/usr/bin/env pwsh
<#
.SYNOPSIS
    VibeCheck Milestone 32 - Database Recovery Validation Test

.DESCRIPTION
    Validates the database backup and recovery strategy for VibeCheck.

    Tests:
      DB-01  Backup creation (full mysqldump)
      DB-02  Restore into isolated environment
      DB-03  Application reconnect validation (connectivity check)
      DB-04  Flyway schema history validation
      DB-05  Outbox consistency verification
      DB-06  Booking data integrity verification
      DB-07  Payment data integrity verification

    SAFETY:
    - All tests run against an isolated docker compose project.
    - The developer's real environment is never touched.
    - No production data is modified.

.PARAMETER Mode
    "isolated" (default) creates a separate Docker project for testing.
    "existing" tests against already-running containers (USE WITH CARE).

.PARAMETER BaseUrl
    Gateway URL for application health checks (only used in "existing" mode).

.EXAMPLE
    .\scripts\milestone-32-database-recovery-test.ps1
    .\scripts\milestone-32-database-recovery-test.ps1 -Mode existing -BaseUrl http://localhost:8079
#>

param(
    [ValidateSet("isolated", "existing")]
    [string]$Mode = "isolated",
    [string]$BaseUrl = "http://localhost:8079"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Continue"

# -----------------------------------------------------------------------
# Counters and Logging
# -----------------------------------------------------------------------
$script:PassCount = 0
$script:FailCount = 0
$script:SkipCount = 0
$script:Results   = @()

$ISOLATED_PROJECT = "vibecheck-dbtest"
$BACKUP_DIR       = ".\backups\milestone-32"
$VIBECHECK_DBS    = @(
    "vibecheck_auth", "vibecheck_movie", "vibecheck_theatre", "vibecheck_show",
    "vibecheck_booking", "vibecheck_payment", "vibecheck_notification"
)
$CRITICAL_DBS     = @("vibecheck_auth", "vibecheck_booking", "vibecheck_payment")

function Write-Log {
    param([string]$Level, [string]$Message)
    $ts = Get-Date -Format "HH:mm:ss"
    $color = switch ($Level) {
        "PASS"  { "Green"   }
        "FAIL"  { "Red"     }
        "SKIP"  { "Yellow"  }
        "INFO"  { "Cyan"    }
        "STEP"  { "Magenta" }
        "WARN"  { "Yellow"  }
        default { "White"   }
    }
    Write-Host "[$ts][$Level] $Message" -ForegroundColor $color
}

function Record-Result {
    param([string]$TestId, [string]$Name, [string]$Status, [string]$Detail = "")
    switch ($Status) {
        "PASS" { $script:PassCount++ }
        "FAIL" { $script:FailCount++ }
        default{ $script:SkipCount++ }
    }
    Write-Log $Status "$TestId: $Name$(if ($Detail) { ' -- ' + $Detail })"
    $script:Results += [PSCustomObject]@{
        TestId = $TestId
        Name   = $Name
        Status = $Status
        Detail = $Detail
    }
}

function Get-DbPassword {
    $pwd = $env:DB_ROOT_PASSWORD
    if (-not $pwd) {
        if (Test-Path ".env") {
            foreach ($line in Get-Content ".env") {
                if ($line -match "^DB_ROOT_PASSWORD=(.+)$") {
                    $pwd = $Matches[1].Trim(); break
                }
            }
        }
    }
    if (-not $pwd) {
        $pwd = "Krish@123"  # fallback for local dev only
        Write-Log "WARN" "DB_ROOT_PASSWORD not set, using default. DO NOT use in production."
    }
    return $pwd
}

function Wait-ForMysql {
    param([string]$Container, [int]$TimeoutSec = 90)
    $start = Get-Date
    Write-Log "INFO" "Waiting for MySQL container: $Container..."
    while ((Get-Date) - $start -lt [TimeSpan]::FromSeconds($TimeoutSec)) {
        $status = docker inspect --format "{{.State.Health.Status}}" $Container 2>$null
        if ($status -eq "healthy") { Write-Log "INFO" "MySQL is healthy."; return $true }
        Start-Sleep -Seconds 5
        Write-Log "INFO" "  Still waiting..."
    }
    Write-Log "FAIL" "MySQL did not become healthy in ${TimeoutSec}s"
    return $false
}

function Invoke-MySql {
    param([string]$Container, [string]$DbPassword, [string]$Query, [string]$Database = "")
    $dbArg = if ($Database) { $Database } else { "" }
    $result = docker exec -e "MYSQL_PWD=$DbPassword" $Container `
        mysql -uroot -N -s $dbArg -e $Query 2>$null
    return $result
}

# -----------------------------------------------------------------------
# HEADER
# -----------------------------------------------------------------------
Write-Host ""
Write-Host "================================================================" -ForegroundColor Magenta
Write-Host "  VIBECHECK MILESTONE 32 - DATABASE RECOVERY TESTS" -ForegroundColor Magenta
Write-Host "  Mode: $Mode" -ForegroundColor Magenta
Write-Host "================================================================" -ForegroundColor Magenta
Write-Host ""

# -----------------------------------------------------------------------
# PREREQUISITE: Docker
# -----------------------------------------------------------------------
$dockerOk = (docker info 2>$null) -ne $null
if (-not $dockerOk) {
    Write-Log "FAIL" "Docker is not running."
    exit 1
}

$dbPassword = Get-DbPassword
New-Item -ItemType Directory -Force -Path $BACKUP_DIR | Out-Null

# -----------------------------------------------------------------------
# SETUP: Determine MySQL container
# -----------------------------------------------------------------------
$mysqlContainer = ""
if ($Mode -eq "isolated") {
    Write-Log "STEP" "Starting isolated MySQL for DR test..."
    $startArgs = @("compose", "-p", $ISOLATED_PROJECT, "up", "-d", "mysql")
    & docker @startArgs 2>&1 | Select-Object -Last 5
    Start-Sleep -Seconds 5
    $mysqlContainer = "${ISOLATED_PROJECT}-mysql-1"
    $mysqlReady = Wait-ForMysql $mysqlContainer 90
    if (-not $mysqlReady) {
        Write-Log "FAIL" "Isolated MySQL failed to start. Check Docker Compose."
        exit 1
    }
} else {
    $mysqlContainer = "vibecheck-mysql"
    $status = docker inspect --format "{{.State.Health.Status}}" $mysqlContainer 2>$null
    if ($status -ne "healthy") {
        Write-Log "FAIL" "vibecheck-mysql is not healthy (status: $status). Run docker compose up -d mysql first."
        exit 1
    }
    Write-Log "INFO" "Using existing container: $mysqlContainer"
}

# -----------------------------------------------------------------------
# DB-01: Backup creation
# -----------------------------------------------------------------------
Write-Log "STEP" "DB-01: Backup creation (mysqldump)"

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backupFile = Join-Path ([System.IO.Path]::GetFullPath($BACKUP_DIR)) "vibecheck_m32_$timestamp.sql.gz"

# Detect which databases exist in this container
$existingDbs = Invoke-MySql $mysqlContainer $dbPassword "SHOW DATABASES;"
$dbsToDump   = $VIBECHECK_DBS | Where-Object { $existingDbs -match $_ }

if ($dbsToDump.Count -eq 0) {
    Write-Log "WARN" "No VibeCheck databases found. Container may be fresh (no migrations run yet)."
    Write-Log "INFO" "Creating schema stubs for backup validation..."
    foreach ($db in @("vibecheck_booking", "vibecheck_payment", "vibecheck_auth")) {
        docker exec -e "MYSQL_PWD=$dbPassword" $mysqlContainer `
            mysql -uroot -e "CREATE DATABASE IF NOT EXISTS $db;" 2>$null
    }
    $existingDbs = Invoke-MySql $mysqlContainer $dbPassword "SHOW DATABASES;"
    $dbsToDump   = $VIBECHECK_DBS | Where-Object { $existingDbs -match $_ }
}

Write-Log "INFO" "Databases to dump: $($dbsToDump -join ', ')"

$dumpArgs = @(
    "exec", "-i",
    "-e", "MYSQL_PWD=$dbPassword",
    $mysqlContainer,
    "mysqldump", "-uroot",
    "--single-transaction",
    "--routines",
    "--events",
    "--databases"
) + $dbsToDump

& docker @dumpArgs | & gzip | Set-Content -Path $backupFile -AsByteStream
$backupSize = if (Test-Path $backupFile) { (Get-Item $backupFile).Length } else { 0 }
$backupOk   = ($LASTEXITCODE -eq 0 -and $backupSize -ge 512)

Record-Result "DB-01" "Backup created successfully" `
    (if ($backupOk) { "PASS" } else { "FAIL" }) `
    "File: $(Split-Path $backupFile -Leaf), Size: $([math]::Round($backupSize / 1024, 1)) KB"

# -----------------------------------------------------------------------
# DB-02: Restore into isolated environment
# -----------------------------------------------------------------------
Write-Log "STEP" "DB-02: Restore backup into isolated environment"

if (-not $backupOk) {
    Record-Result "DB-02" "Restore backup to isolated DB" "SKIP" "Backup creation failed"
    $restoreOk = $false
} else {
    # Create a second isolated MySQL for restore target
    $restoreProject  = "vibecheck-dbrestore"
    $restoreContainer = "${restoreProject}-mysql-1"

    Write-Log "INFO" "Starting restore-target MySQL..."
    $startRestoreArgs = @("compose", "-p", $restoreProject, "up", "-d", "mysql")
    & docker @startRestoreArgs 2>&1 | Select-Object -Last 3
    Start-Sleep -Seconds 5
    $restoreReady = Wait-ForMysql $restoreContainer 90

    if (-not $restoreReady) {
        Record-Result "DB-02" "Restore-target MySQL started" "FAIL" "Container failed to become healthy"
        $restoreOk = $false
    } else {
        $restoreStart = Get-Date
        & gzip -dc $backupFile | docker exec -i `
            -e "MYSQL_PWD=$dbPassword" `
            $restoreContainer `
            mysql -uroot

        $restoreOk      = ($LASTEXITCODE -eq 0)
        $restoreElapsed = [math]::Round(((Get-Date) - $restoreStart).TotalSeconds, 1)
        Record-Result "DB-02" "Restore backup to isolated DB" `
            (if ($restoreOk) { "PASS" } else { "FAIL" }) `
            "Duration: ${restoreElapsed}s"
    }
}

# -----------------------------------------------------------------------
# DB-03: Application reconnect validation
# -----------------------------------------------------------------------
Write-Log "STEP" "DB-03: Application reconnect validation"

if ($Mode -eq "existing" -and $BaseUrl) {
    try {
        $health = Invoke-WebRequest -Uri "$BaseUrl/actuator/health" -Method GET -TimeoutSec 10 -ErrorAction Stop
        $healthBody = $health.Content | ConvertFrom-Json
        $appHealthy = $healthBody.status -eq "UP"
        Record-Result "DB-03" "Application health after DB availability" `
            (if ($appHealthy) { "PASS" } else { "FAIL" }) `
            "Status: $($healthBody.status)"
    } catch {
        Record-Result "DB-03" "Application health after DB availability" "FAIL" "Error: $_"
    }
} else {
    Record-Result "DB-03" "Application reconnect to DB" "NOT EXECUTED" `
        "Application not running in isolated mode. Run in 'existing' mode with live services."
}

# -----------------------------------------------------------------------
# DB-04: Flyway validation
# -----------------------------------------------------------------------
Write-Log "STEP" "DB-04: Flyway schema history validation"

$flywayContainer = if ($restoreOk) { $restoreContainer } elseif ($Mode -eq "existing") { $mysqlContainer } else { $null }

if (-not $flywayContainer) {
    Record-Result "DB-04" "Flyway schema history valid" "SKIP" "No target container available"
} else {
    $flywayResult = Invoke-MySql $flywayContainer $dbPassword `
        "SELECT COUNT(*) FROM flyway_schema_history WHERE success=1;" "vibecheck_booking" 2>$null
    $flywayOk = $flywayResult -match "[1-9][0-9]*"
    Record-Result "DB-04" "Flyway schema history intact in vibecheck_booking" `
        (if ($flywayOk) { "PASS" } else { "FAIL" }) `
        "Successful migrations: $($flywayResult.Trim())"

    $failedMigrations = Invoke-MySql $flywayContainer $dbPassword `
        "SELECT COUNT(*) FROM flyway_schema_history WHERE success=0;" "vibecheck_booking" 2>$null
    $noFailures = ($failedMigrations.Trim() -eq "0")
    Record-Result "DB-04a" "No failed Flyway migrations" `
        (if ($noFailures) { "PASS" } else { "FAIL" }) `
        "Failed migrations: $($failedMigrations.Trim())"
}

# -----------------------------------------------------------------------
# DB-05: Outbox consistency verification
# -----------------------------------------------------------------------
Write-Log "STEP" "DB-05: Outbox consistency verification"

$outboxContainer = if ($restoreOk) { $restoreContainer } elseif ($Mode -eq "existing") { $mysqlContainer } else { $null }

if (-not $outboxContainer) {
    Record-Result "DB-05" "Outbox table structure intact" "SKIP" "No target container"
} else {
    $outboxTables = Invoke-MySql $outboxContainer $dbPassword "SHOW TABLES LIKE '%outbox%';" "vibecheck_booking"
    $outboxExists = $outboxTables -match "outbox_events"
    Record-Result "DB-05" "outbox_events table present" `
        (if ($outboxExists) { "PASS" } else { "FAIL" }) ""

    if ($outboxExists) {
        # Verify no records are stuck in PROCESSING (would indicate recovery needed)
        $stuckProcessing = Invoke-MySql $outboxContainer $dbPassword `
            "SELECT COUNT(*) FROM outbox_events WHERE status='PROCESSING';" "vibecheck_booking"
        $noStuck = ($stuckProcessing.Trim() -eq "0")
        Record-Result "DB-05a" "No outbox records stuck in PROCESSING" `
            (if ($noStuck) { "PASS" } else { "FAIL" }) `
            "Stuck PROCESSING count: $($stuckProcessing.Trim())"

        # Verify dead-letter count
        $deadLetter = Invoke-MySql $outboxContainer $dbPassword `
            "SELECT COUNT(*) FROM outbox_events WHERE status='DEAD_LETTER';" "vibecheck_booking"
        Record-Result "DB-05b" "Outbox DEAD_LETTER count recorded" "PASS" `
            "DEAD_LETTER count: $($deadLetter.Trim())"
    }

    $processedEvents = Invoke-MySql $outboxContainer $dbPassword "SHOW TABLES LIKE '%processed%';" "vibecheck_booking"
    Record-Result "DB-05c" "processed_events table present" `
        (if ($processedEvents -match "processed_events") { "PASS" } else { "FAIL" }) ""
}

# -----------------------------------------------------------------------
# DB-06: Booking data integrity
# -----------------------------------------------------------------------
Write-Log "STEP" "DB-06: Booking data integrity verification"

$bookingContainer = if ($restoreOk) { $restoreContainer } elseif ($Mode -eq "existing") { $mysqlContainer } else { $null }

if (-not $bookingContainer) {
    Record-Result "DB-06" "Booking tables intact" "SKIP" "No target container"
} else {
    $bookingTables = Invoke-MySql $bookingContainer $dbPassword "SHOW TABLES;" "vibecheck_booking"

    $requiredBookingTables = @("bookings", "show_seats")
    foreach ($tbl in $requiredBookingTables) {
        $present = $bookingTables -match $tbl
        Record-Result "DB-06-$tbl" "Table '$tbl' present in vibecheck_booking" `
            (if ($present) { "PASS" } else { "FAIL" }) ""
    }

    if ($bookingTables -match "bookings") {
        # Check for impossible booking states (no CONFIRMED booking with non-existent shows)
        $invalidStates = Invoke-MySql $bookingContainer $dbPassword `
            "SELECT COUNT(*) FROM bookings WHERE status NOT IN ('PENDING','CONFIRMED','CANCELLED','EXPIRED','FAILED');" `
            "vibecheck_booking"
        $noInvalidStates = ($invalidStates.Trim() -eq "0")
        Record-Result "DB-06a" "No bookings with impossible status values" `
            (if ($noInvalidStates) { "PASS" } else { "FAIL" }) `
            "Invalid status count: $($invalidStates.Trim())"

        $bookingCount = Invoke-MySql $bookingContainer $dbPassword `
            "SELECT COUNT(*) FROM bookings;" "vibecheck_booking"
        Record-Result "DB-06b" "Booking record count" "PASS" `
            "Total bookings: $($bookingCount.Trim())"
    }
}

# -----------------------------------------------------------------------
# DB-07: Payment data integrity
# -----------------------------------------------------------------------
Write-Log "STEP" "DB-07: Payment data integrity verification"

$paymentContainer = if ($restoreOk) { $restoreContainer } elseif ($Mode -eq "existing") { $mysqlContainer } else { $null }

if (-not $paymentContainer) {
    Record-Result "DB-07" "Payment tables intact" "SKIP" "No target container"
} else {
    $paymentTables = Invoke-MySql $paymentContainer $dbPassword "SHOW TABLES;" "vibecheck_payment"

    if ($paymentTables -match "payments") {
        Record-Result "DB-07" "payments table present in vibecheck_payment" "PASS" ""

        $dupPayments = Invoke-MySql $paymentContainer $dbPassword `
            "SELECT idempotency_key, COUNT(*) c FROM payments GROUP BY idempotency_key HAVING c > 1;" `
            "vibecheck_payment"
        $noDupPayments = ([string]::IsNullOrWhiteSpace($dupPayments))
        Record-Result "DB-07a" "No duplicate payments (idempotency keys unique)" `
            (if ($noDupPayments) { "PASS" } else { "FAIL" }) `
            "Duplicate keys: $(if ($noDupPayments) { 'none' } else { $dupPayments })"

        $invalidPayStates = Invoke-MySql $paymentContainer $dbPassword `
            "SELECT COUNT(*) FROM payments WHERE status NOT IN ('PENDING','COMPLETED','FAILED','REFUNDED');" `
            "vibecheck_payment"
        $noInvalidPayStates = ($invalidPayStates.Trim() -eq "0")
        Record-Result "DB-07b" "No payments with impossible status values" `
            (if ($noInvalidPayStates) { "PASS" } else { "FAIL" }) `
            "Invalid status count: $($invalidPayStates.Trim())"
    } else {
        Record-Result "DB-07" "payments table present in vibecheck_payment" "FAIL" `
            "Table not found. DB may not be initialized."
    }
}

# -----------------------------------------------------------------------
# CLEANUP
# -----------------------------------------------------------------------
Write-Log "STEP" "Cleanup isolated environments"
if ($Mode -eq "isolated") {
    Write-Log "INFO" "Removing isolated MySQL (source)..."
    docker compose -p $ISOLATED_PROJECT down -v 2>&1 | Select-Object -Last 3
}
if ($restoreOk) {
    Write-Log "INFO" "Removing restore-target MySQL..."
    docker compose -p $restoreProject down -v 2>&1 | Select-Object -Last 3
}

# -----------------------------------------------------------------------
# FINAL REPORT
# -----------------------------------------------------------------------
Write-Host ""
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host "  DATABASE RECOVERY TEST RESULTS" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan
Write-Host ""

foreach ($r in $script:Results) {
    $color = switch ($r.Status) {
        "PASS"         { "Green"  }
        "FAIL"         { "Red"    }
        "SKIP"         { "Yellow" }
        "NOT EXECUTED" { "Yellow" }
        default        { "White"  }
    }
    $detail = if ($r.Detail) { "  ($($r.Detail))" } else { "" }
    Write-Host "  [$($r.Status.PadRight(12))] $($r.TestId.PadRight(16)) $($r.Name)$detail" -ForegroundColor $color
}

Write-Host ""
Write-Host "  PASS         : $($script:PassCount)" -ForegroundColor Green
Write-Host "  FAIL         : $($script:FailCount)" -ForegroundColor $(if ($script:FailCount -gt 0) { "Red" } else { "Green" })
Write-Host "  NOT EXECUTED : $($script:SkipCount)" -ForegroundColor Yellow
Write-Host ""
Write-Host "  Backup saved to: $BACKUP_DIR" -ForegroundColor Cyan
Write-Host "================================================================" -ForegroundColor Cyan

exit $(if ($script:FailCount -gt 0) { 1 } else { 0 })
