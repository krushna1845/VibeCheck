#!/usr/bin/env pwsh
<#
.SYNOPSIS
    VibeCheck MySQL Backup Verification Script - Phase 5, Milestone 31

.DESCRIPTION
    Verifies the integrity of a VibeCheck MySQL backup file without
    touching the production database.

    Verification checks:
      1. File exists and is non-empty
      2. File is a valid gzip archive
      3. Decompressed content is valid SQL (mysqldump format)
      4. Expected databases are present in the dump
      5. Expected tables/schema information exists in the dump
      6. Optionally: restore into a disposable Docker container for full verification

.PARAMETER BackupFile
    Path to the .sql.gz backup file to verify.
    If not specified, verifies the most recent backup in BackupDir.

.PARAMETER BackupDir
    Directory to search for backup files (default: ./backups/mysql)

.PARAMETER FullVerify
    Switch. If specified, spins up a temporary MySQL container, restores
    the backup, verifies schema, then destroys the container.
    Requires Docker. Takes several minutes.

.EXAMPLE
    # Verify the latest backup
    .\scripts\backup\verify-backup.ps1

    # Verify a specific backup
    .\scripts\backup\verify-backup.ps1 -BackupFile ".\backups\mysql\vibecheck_backup_20260926_120000.sql.gz"

    # Full verification (restore into disposable container)
    .\scripts\backup\verify-backup.ps1 -FullVerify
#>

param(
    [string]$BackupFile = "",
    [string]$BackupDir = ".\backups\mysql",
    [switch]$FullVerify
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

# -----------------------------------------------------------------------------
# Logging helpers
# -----------------------------------------------------------------------------
function Write-Log {
    param([string]$Level, [string]$Message)
    $ts = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
    $color = switch ($Level) {
        "INFO"  { "Cyan"   }
        "WARN"  { "Yellow" }
        "ERROR" { "Red"    }
        "OK"    { "Green"  }
        "CHECK" { "Blue"   }
        default { "White"  }
    }
    Write-Host "[$ts] [$Level] $Message" -ForegroundColor $color
}

function Write-Info  { param([string]$m) Write-Log "INFO"  $m }
function Write-Warn  { param([string]$m) Write-Log "WARN"  $m }
function Write-Err   { param([string]$m) Write-Log "ERROR" $m }
function Write-Ok    { param([string]$m) Write-Log "OK"    $m }
function Write-Check { param([string]$m) Write-Log "CHECK" $m }

$script:PassCount = 0
$script:FailCount = 0
$script:WarnCount = 0

function Assert-Check {
    param([string]$Name, [bool]$Passed, [string]$Detail = "")
    if ($Passed) {
        $script:PassCount++
        Write-Ok "  PASS: $Name$(if ($Detail) { " - $Detail" })"
    } else {
        $script:FailCount++
        Write-Err "  FAIL: $Name$(if ($Detail) { " - $Detail" })"
    }
}

function Warn-Check {
    param([string]$Name, [string]$Detail = "")
    $script:WarnCount++
    Write-Warn "  WARN: $Name$(if ($Detail) { " - $Detail" })"
}

# -----------------------------------------------------------------------------
# Resolve backup file
# -----------------------------------------------------------------------------
function Resolve-BackupFile {
    if ($BackupFile -ne "") {
        return [System.IO.Path]::GetFullPath($BackupFile)
    }

    $resolvedDir = [System.IO.Path]::GetFullPath($BackupDir)
    if (-not (Test-Path $resolvedDir)) {
        Write-Err "Backup directory not found: $resolvedDir"
        Write-Err "Run: .\scripts\backup\mysql-backup.ps1 -Action backup"
        exit 1
    }

    $latest = Get-ChildItem -Path $resolvedDir -Filter "vibecheck_backup_*.sql.gz" |
              Sort-Object LastWriteTime -Descending |
              Select-Object -First 1

    if ($null -eq $latest) {
        Write-Err "No backup files found in: $resolvedDir"
        exit 1
    }

    Write-Info "Using most recent backup: $($latest.Name)"
    return $latest.FullName
}

# -----------------------------------------------------------------------------
# CHECK 1: File existence and size
# -----------------------------------------------------------------------------
function Test-FileExistence {
    param([string]$Path)

    Write-Check "CHECK 1: File existence and size"

    $exists = Test-Path $Path
    Assert-Check "File exists" $exists $Path

    if (-not $exists) { exit 1 }

    $fileInfo = Get-Item $Path
    $sizeOk = $fileInfo.Length -ge 1024

    Write-Info "  File size: $([math]::Round($fileInfo.Length / 1MB, 3)) MB ($($fileInfo.Length) bytes)"
    Assert-Check "File is non-empty (>= 1 KB)" $sizeOk "Size: $($fileInfo.Length) bytes"

    return $fileInfo
}

# -----------------------------------------------------------------------------
# CHECK 2: Valid gzip archive
# -----------------------------------------------------------------------------
function Test-GzipValidity {
    param([string]$Path)

    Write-Check "CHECK 2: Valid gzip archive (magic bytes)"

    try {
        $bytes = [System.IO.File]::ReadAllBytes($Path)
        $validMagic = ($bytes.Count -ge 2 -and $bytes[0] -eq 0x1F -and $bytes[1] -eq 0x8B)
        Assert-Check "Valid gzip magic bytes (1F 8B)" $validMagic `
            "Bytes[0-1]: $($bytes[0].ToString('X2')) $($bytes[1].ToString('X2'))"
    } catch {
        Assert-Check "Valid gzip magic bytes" $false "Error reading file: $_"
    }
}

# -----------------------------------------------------------------------------
# CHECK 3: Valid mysqldump SQL content
# -----------------------------------------------------------------------------
function Test-SqlContent {
    param([string]$Path)

    Write-Check "CHECK 3: Valid mysqldump SQL content"

    try {
        # Read first 50 lines of decompressed content
        $lines = & gzip -dc $Path 2>$null | Select-Object -First 50
        $content = $lines -join "`n"

        $hasMySQLDump = $content -match "mysqldump|MySQL dump|MariaDB dump"
        Assert-Check "Contains mysqldump header" $hasMySQLDump

        $hasCreateDB = $content -match "CREATE DATABASE"
        Assert-Check "Contains CREATE DATABASE statements" $hasCreateDB

        $hasVibecheck = $content -match "vibecheck_"
        Assert-Check "Contains vibecheck_ database references" $hasVibecheck

    } catch {
        Assert-Check "SQL content readable" $false "gzip tool error: $_"
        Warn-Check "gzip not in PATH" "Install gzip for Windows or use WSL"
    }
}

# -----------------------------------------------------------------------------
# CHECK 4: Expected databases present in dump
# -----------------------------------------------------------------------------
function Test-DatabasePresence {
    param([string]$Path)

    Write-Check "CHECK 4: Expected databases in dump"

    $expectedDbs = @(
        "vibecheck_auth",
        "vibecheck_movie",
        "vibecheck_theatre",
        "vibecheck_show",
        "vibecheck_booking",
        "vibecheck_payment",
        "vibecheck_notification"
    )

    try {
        # Scan more lines for database presence
        $content = & gzip -dc $Path 2>$null | Select-Object -First 500 | Out-String

        foreach ($db in $expectedDbs) {
            $found = $content -match $db
            Assert-Check "Database in dump: $db" $found
        }
    } catch {
        Warn-Check "Could not scan database list" "gzip tool may not be available: $_"
    }
}

# -----------------------------------------------------------------------------
# CHECK 5: Expected tables/schema present
# -----------------------------------------------------------------------------
function Test-TablePresence {
    param([string]$Path)

    Write-Check "CHECK 5: Expected tables in dump"

    $keyTables = @(
        "bookings",
        "booking_seats",
        "outbox_events",
        "processed_events",
        "users",
        "movies",
        "shows"
    )

    try {
        # Scan first 2000 lines for table presence (CREATE TABLE statements)
        $content = & gzip -dc $Path 2>$null | Select-Object -First 2000 | Out-String

        foreach ($table in $keyTables) {
            # Look for CREATE TABLE `tablename` pattern
            $found = $content -match "CREATE TABLE.*`$table`|Table structure for.*$table"
            if ($found) {
                Assert-Check "Table schema present: $table" $true
            } else {
                # Warn (not fail) since some tables may not have data yet
                Warn-Check "Table schema not found in first 2000 lines: $table" "(may exist deeper in dump or table may not exist yet)"
            }
        }
    } catch {
        Warn-Check "Could not scan table list" "gzip tool may not be available: $_"
    }
}

# -----------------------------------------------------------------------------
# CHECK 6 (Optional): Full restore into disposable container
# -----------------------------------------------------------------------------
function Test-FullRestore {
    param([string]$Path)

    Write-Check "CHECK 6: Full restore into disposable container"
    Write-Warn "  This will start a temporary MySQL container. This may take 2-4 minutes."

    $tempContainer = "vibecheck-verify-$(Get-Date -Format 'yyyyMMddHHmmss')"
    $tempPassword = "VerifyTempPassword123"

    try {
        # Start temporary MySQL container
        Write-Info "  Starting temporary container: $tempContainer"
        docker run -d `
            --name $tempContainer `
            -e "MYSQL_ROOT_PASSWORD=$tempPassword" `
            --rm `
            mysql:8.0 | Out-Null

        if ($LASTEXITCODE -ne 0) {
            Assert-Check "Start temporary MySQL container" $false "docker run failed"
            return
        }

        # Wait for MySQL to be ready (up to 60 seconds)
        Write-Info "  Waiting for temporary MySQL to be ready..."
        $ready = $false
        for ($i = 0; $i -lt 30; $i++) {
            Start-Sleep -Seconds 2
            $ping = docker exec -e "MYSQL_PWD=$tempPassword" $tempContainer `
                mysqladmin ping -uroot 2>$null
            if ($LASTEXITCODE -eq 0) {
                $ready = $true
                break
            }
        }

        Assert-Check "Temporary MySQL container is ready" $ready

        if (-not $ready) {
            Write-Err "  Temporary MySQL did not become ready in time."
            return
        }

        # Restore backup into temporary container
        Write-Info "  Restoring backup into temporary container..."
        & gzip -dc $Path | docker exec -i `
            -e "MYSQL_PWD=$tempPassword" `
            $tempContainer `
            mysql -uroot

        $restoreOk = ($LASTEXITCODE -eq 0)
        Assert-Check "Backup restores without errors" $restoreOk "Exit code: $LASTEXITCODE"

        if ($restoreOk) {
            # Verify databases exist
            $dbs = docker exec -e "MYSQL_PWD=$tempPassword" $tempContainer `
                mysql -uroot -e "SHOW DATABASES;" 2>&1 | Out-String

            $expectedDbs = @("vibecheck_booking", "vibecheck_payment", "vibecheck_auth")
            foreach ($db in $expectedDbs) {
                $found = $dbs -match $db
                Assert-Check "Database restorable: $db" $found
            }

            # Check Flyway history
            $flyway = docker exec -e "MYSQL_PWD=$tempPassword" $tempContainer `
                mysql -uroot -e "SELECT COUNT(*) FROM vibecheck_booking.flyway_schema_history WHERE success=1;" 2>&1

            $hasMigrations = $flyway -match "[1-9][0-9]*"
            Assert-Check "Flyway migration history present" $hasMigrations $flyway
        }

    } finally {
        # Always clean up temporary container
        Write-Info "  Cleaning up temporary container: $tempContainer"
        docker stop $tempContainer 2>$null
        docker rm $tempContainer 2>$null
        Write-Info "  Temporary container removed."
    }
}

# -----------------------------------------------------------------------------
# Main
# -----------------------------------------------------------------------------
Write-Info "VibeCheck Backup Verification - Milestone 31, Phase 5"
Write-Host ""

$resolvedFile = Resolve-BackupFile
Write-Info "Verifying: $resolvedFile"
Write-Host ""

$fileInfo = Test-FileExistence -Path $resolvedFile
Write-Host ""

Test-GzipValidity -Path $resolvedFile
Write-Host ""

Test-SqlContent -Path $resolvedFile
Write-Host ""

Test-DatabasePresence -Path $resolvedFile
Write-Host ""

Test-TablePresence -Path $resolvedFile
Write-Host ""

if ($FullVerify) {
    Test-FullRestore -Path $resolvedFile
    Write-Host ""
}

# -----------------------------------------------------------------------------
# Summary
# -----------------------------------------------------------------------------
Write-Host "=================================================================="
Write-Host "VERIFICATION SUMMARY" -ForegroundColor White
Write-Host "=================================================================="
Write-Ok   "  PASSED : $($script:PassCount)"
Write-Warn "  WARNED : $($script:WarnCount)"
if ($script:FailCount -gt 0) {
    Write-Err "  FAILED : $($script:FailCount)"
} else {
    Write-Host "  FAILED : 0" -ForegroundColor Green
}
Write-Host "=================================================================="

if ($script:FailCount -gt 0) {
    Write-Err "RESULT: FAILED - $($script:FailCount) check(s) failed. Backup may be corrupt or incomplete."
    exit 1
} elseif ($script:WarnCount -gt 0) {
    Write-Warn "RESULT: PASSED WITH WARNINGS - $($script:WarnCount) warning(s). Review above."
    exit 0
} else {
    Write-Ok "RESULT: PASSED - All checks passed. Backup appears valid."
    exit 0
}
