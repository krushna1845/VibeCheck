#!/usr/bin/env pwsh
<#
.SYNOPSIS
    VibeCheck MySQL Restore Script - Phase 4, Milestone 31

.DESCRIPTION
    Restores a VibeCheck MySQL backup into the running vibecheck-mysql
    Docker container.

    SAFETY REQUIREMENTS:
      - Requires explicit backup file path argument
      - Requires --confirm flag to proceed (prevents accidental restores)
      - Validates backup file integrity before restoring
      - Performs post-restore schema verification
      - Never silently overwrites the database

    Credentials are read exclusively from environment variables or .env file.

.PARAMETER BackupFile
    REQUIRED. Full path to the .sql.gz backup file to restore.

.PARAMETER Confirm
    Switch. Must be explicitly provided to allow restore to proceed.
    This is a safety mechanism to prevent accidental database overwrites.

.PARAMETER ContainerName
    Docker container name for MySQL (default: vibecheck-mysql)

.PARAMETER SkipVerification
    Skip post-restore schema verification (not recommended).

.EXAMPLE
    # List available backups first
    .\scripts\backup\mysql-backup.ps1 -Action list

    # Restore a specific backup (requires explicit confirmation)
    .\scripts\backup\mysql-restore.ps1 `
        -BackupFile ".\backups\mysql\vibecheck_backup_20260926_120000.sql.gz" `
        -Confirm

    # Restore with custom container name
    .\scripts\backup\mysql-restore.ps1 `
        -BackupFile ".\backups\mysql\vibecheck_backup_20260926_120000.sql.gz" `
        -ContainerName "vibecheck-mysql" `
        -Confirm

.NOTES
    NEVER run this against a live production database without:
    1. Taking a fresh backup first
    2. Notifying stakeholders of the maintenance window
    3. Verifying the backup file is from the correct point in time
    4. Confirming the restore with your team
#>

param(
    [Parameter(Mandatory = $true, HelpMessage = "Path to the .sql.gz backup file to restore")]
    [string]$BackupFile,

    [switch]$Confirm,

    [string]$ContainerName = "vibecheck-mysql",

    [switch]$SkipVerification
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
        "INFO"  { "Cyan"    }
        "WARN"  { "Yellow"  }
        "ERROR" { "Red"     }
        "OK"    { "Green"   }
        "ALERT" { "Magenta" }
        default { "White"   }
    }
    Write-Host "[$ts] [$Level] $Message" -ForegroundColor $color
}

function Write-Info  { param([string]$m) Write-Log "INFO"  $m }
function Write-Warn  { param([string]$m) Write-Log "WARN"  $m }
function Write-Err   { param([string]$m) Write-Log "ERROR" $m }
function Write-Ok    { param([string]$m) Write-Log "OK"    $m }
function Write-Alert { param([string]$m) Write-Log "ALERT" $m }

# -----------------------------------------------------------------------------
# Resolve DB credentials
# -----------------------------------------------------------------------------
function Get-DbPassword {
    $pwd = $env:DB_ROOT_PASSWORD
    if ($pwd) {
        Write-Info "Using DB_ROOT_PASSWORD from environment variable."
        return $pwd
    }

    $envFile = Join-Path $PSScriptRoot "..\..\..\.env"
    if (-not (Test-Path $envFile)) {
        $envFile = Join-Path (Get-Location) ".env"
    }

    if (Test-Path $envFile) {
        Write-Info "Loading credentials from: $envFile"
        foreach ($line in Get-Content $envFile) {
            if ($line -match "^DB_ROOT_PASSWORD=(.+)$") {
                return $Matches[1].Trim()
            }
        }
    }

    Write-Err "DB_ROOT_PASSWORD not set in environment or .env file."
    exit 1
}

# -----------------------------------------------------------------------------
# Verify container is running
# -----------------------------------------------------------------------------
function Assert-ContainerRunning {
    param([string]$Name)
    $status = docker inspect --format '{{.State.Status}}' $Name 2>$null
    if ($LASTEXITCODE -ne 0 -or $status -ne "running") {
        Write-Err "Container '$Name' is not running (status: $status)."
        Write-Err "Start MySQL with: docker compose up -d mysql"
        exit 1
    }
    Write-Info "Container '$Name' is running."
}

# -----------------------------------------------------------------------------
# Pre-restore validation
# -----------------------------------------------------------------------------
function Test-BackupFile {
    param([string]$Path)

    # Check file exists
    if (-not (Test-Path $Path)) {
        Write-Err "Backup file not found: $Path"
        exit 1
    }

    $fileInfo = Get-Item $Path
    Write-Info "Backup file     : $($fileInfo.FullName)"
    Write-Info "Backup size     : $([math]::Round($fileInfo.Length / 1MB, 2)) MB"
    Write-Info "Backup created  : $($fileInfo.LastWriteTime)"

    # Check file is non-empty
    if ($fileInfo.Length -lt 1024) {
        Write-Err "Backup file is suspiciously small ($($fileInfo.Length) bytes). Aborting."
        exit 1
    }

    # Verify it is a valid gzip archive
    try {
        $bytes = [System.IO.File]::ReadAllBytes($fileInfo.FullName)
        # gzip magic bytes: 1F 8B
        if ($bytes[0] -ne 0x1F -or $bytes[1] -ne 0x8B) {
            Write-Err "File does not appear to be a valid gzip archive (magic bytes mismatch)."
            Write-Err "Expected: 1F 8B, Got: $($bytes[0].ToString('X2')) $($bytes[1].ToString('X2'))"
            exit 1
        }
        Write-Ok "Backup file gzip header: VALID"
    } catch {
        Write-Err "Failed to read backup file: $_"
        exit 1
    }

    # Check it contains expected SQL content by peeking at decompressed header
    try {
        $decompressedHeader = & gzip -dc $fileInfo.FullName 2>$null | Select-Object -First 5
        $hasMySQLDump = $decompressedHeader | Where-Object { $_ -match "MySQL dump|MariaDB dump|CREATE DATABASE|mysqldump" }
        if ($hasMySQLDump) {
            Write-Ok "Backup SQL content: VALID (MySQL dump detected)"
        } else {
            Write-Warn "Could not detect MySQL dump markers in first 5 lines. Proceeding with caution."
        }
    } catch {
        Write-Warn "Could not verify backup content (gzip tool may not be in PATH): $_"
    }
}

# -----------------------------------------------------------------------------
# Post-restore verification
# -----------------------------------------------------------------------------
function Invoke-PostRestoreVerification {
    param([string]$DbPassword)

    Write-Info "Running post-restore verification..."

    $expectedDatabases = @(
        "vibecheck_auth",
        "vibecheck_movie",
        "vibecheck_theatre",
        "vibecheck_show",
        "vibecheck_booking",
        "vibecheck_payment",
        "vibecheck_notification"
    )

    $result = docker exec -e "MYSQL_PWD=$DbPassword" $ContainerName `
        mysql -uroot -e "SHOW DATABASES;" 2>&1

    $allPresent = $true
    foreach ($db in $expectedDatabases) {
        if ($result -match $db) {
            Write-Ok "  Database present: $db"
        } else {
            Write-Err "  Database MISSING: $db"
            $allPresent = $false
        }
    }

    # Verify key tables exist in booking DB
    $bookingTables = docker exec -e "MYSQL_PWD=$DbPassword" $ContainerName `
        mysql -uroot -e "SHOW TABLES IN vibecheck_booking;" 2>&1

    $keyTables = @("bookings", "booking_seats", "outbox_events", "processed_events")
    foreach ($table in $keyTables) {
        if ($bookingTables -match $table) {
            Write-Ok "  Table present: vibecheck_booking.$table"
        } else {
            Write-Warn "  Table may be missing: vibecheck_booking.$table (may not exist if no bookings were made)"
        }
    }

    # Check flyway schema history
    $flywayHistory = docker exec -e "MYSQL_PWD=$DbPassword" $ContainerName `
        mysql -uroot -e "SELECT version, description, success FROM vibecheck_booking.flyway_schema_history ORDER BY installed_rank;" 2>&1

    Write-Info "Flyway schema history (vibecheck_booking):"
    Write-Host $flywayHistory

    if ($allPresent) {
        Write-Ok "Post-restore verification: PASSED"
    } else {
        Write-Err "Post-restore verification: FAILED - some databases are missing"
        exit 1
    }
}

# -----------------------------------------------------------------------------
# Main restore procedure
# -----------------------------------------------------------------------------

Write-Alert "=================================================================="
Write-Alert "  VibeCheck MySQL Restore - Milestone 31"
Write-Alert "=================================================================="
Write-Alert ""
Write-Alert "  WARNING: This will OVERWRITE existing database data!"
Write-Alert "  This action is IRREVERSIBLE without another backup."
Write-Alert ""
Write-Alert "  Backup file : $BackupFile"
Write-Alert "  Container   : $ContainerName"
Write-Alert ""
Write-Alert "=================================================================="
Write-Host ""

# Safety gate - require explicit -Confirm flag
if (-not $Confirm) {
    Write-Err "SAFETY: Restore not confirmed. Re-run with -Confirm flag to proceed."
    Write-Err ""
    Write-Err "  Example:"
    Write-Err "    .\scripts\backup\mysql-restore.ps1 -BackupFile `"$BackupFile`" -Confirm"
    Write-Err ""
    Write-Err "  BEFORE RESTORING:"
    Write-Err "    1. Take a fresh backup: .\scripts\backup\mysql-backup.ps1"
    Write-Err "    2. Verify the backup: .\scripts\backup\verify-backup.ps1"
    Write-Err "    3. Notify your team"
    Write-Err "    4. Confirm the restore is for the correct environment"
    exit 1
}

# Resolve absolute path
$resolvedBackupFile = [System.IO.Path]::GetFullPath($BackupFile)

# Step 1: Validate backup file
Write-Info "Step 1: Validating backup file..."
Test-BackupFile -Path $resolvedBackupFile

# Step 2: Check container
Write-Info "Step 2: Verifying container status..."
$dbPassword = Get-DbPassword
Assert-ContainerRunning $ContainerName

# Step 3: Final confirmation prompt
Write-Host ""
Write-Alert "  You are about to restore:"
Write-Alert "    File      : $resolvedBackupFile"
Write-Alert "    Into      : $ContainerName"
Write-Alert "    Databases : ALL vibecheck_* databases"
Write-Host ""
$userConfirm = Read-Host "  Type 'RESTORE' to confirm (anything else aborts)"
if ($userConfirm -ne "RESTORE") {
    Write-Warn "Restore aborted by user."
    exit 0
}

# Step 4: Execute restore
Write-Info "Step 4: Starting restore (decompressing and piping to MySQL)..."
$startTime = Get-Date

# Decompress and pipe to mysql inside the container
& gzip -dc $resolvedBackupFile | docker exec -i `
    -e "MYSQL_PWD=$dbPassword" `
    $ContainerName `
    mysql -uroot

$restoreExitCode = $LASTEXITCODE

$endTime = Get-Date
$elapsed = ($endTime - $startTime).TotalSeconds

if ($restoreExitCode -ne 0) {
    Write-Err "Restore failed with exit code $restoreExitCode."
    Write-Err "MySQL may be in a partially restored state."
    Write-Err "Check MySQL logs: docker logs $ContainerName"
    exit 1
}

Write-Ok "Restore completed in $([math]::Round($elapsed, 1)) seconds."

# Step 5: Verify
if (-not $SkipVerification) {
    Write-Info "Step 5: Running post-restore verification..."
    Invoke-PostRestoreVerification -DbPassword $dbPassword
}

Write-Ok "=================================================================="
Write-Ok "  MySQL Restore: COMPLETE"
Write-Ok "  Duration: $([math]::Round($elapsed, 1)) seconds"
Write-Ok "=================================================================="
Write-Host ""
Write-Info "NEXT STEPS:"
Write-Info "  1. Restart application services to reconnect Flyway:"
Write-Info "     docker compose restart auth-service booking-service payment-service"
Write-Info "  2. Verify application health:"
Write-Info "     curl http://localhost:8079/actuator/health"
Write-Info "  3. Check Flyway migration status:"
Write-Info "     See application startup logs for Flyway validation"

exit 0
