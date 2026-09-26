#!/usr/bin/env pwsh
<#
.SYNOPSIS
    VibeCheck MySQL Backup Script - Phase 3, Milestone 31

.DESCRIPTION
    Performs a logical mysqldump backup of all VibeCheck databases running
    inside the vibecheck-mysql Docker container.

    Supports:
      backup   - take a new compressed backup
      list     - list existing backups
      cleanup  - remove backups older than retention period

    Credentials are read exclusively from environment variables or the
    project .env file. No passwords are hardcoded.

.PARAMETER Action
    backup | list | cleanup  (default: backup)

.PARAMETER BackupDir
    Directory to store backup files (default: ./backups/mysql)

.PARAMETER RetentionDays
    How many days to retain backups before cleanup removes them (default: 7)

.PARAMETER ContainerName
    Docker container name for MySQL (default: vibecheck-mysql)

.EXAMPLE
    # Take a backup (reads DB_ROOT_PASSWORD from .env or environment)
    .\scripts\backup\mysql-backup.ps1 -Action backup

    # List all backups
    .\scripts\backup\mysql-backup.ps1 -Action list

    # Remove backups older than 7 days
    .\scripts\backup\mysql-backup.ps1 -Action cleanup -RetentionDays 7
#>

param(
    [ValidateSet("backup", "list", "cleanup")]
    [string]$Action = "backup",

    [string]$BackupDir = ".\backups\mysql",

    [int]$RetentionDays = 7,

    [string]$ContainerName = "vibecheck-mysql"
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
        default { "White"  }
    }
    Write-Host "[$ts] [$Level] $Message" -ForegroundColor $color
}

function Write-Info  { param([string]$m) Write-Log "INFO"  $m }
function Write-Warn  { param([string]$m) Write-Log "WARN"  $m }
function Write-Err   { param([string]$m) Write-Log "ERROR" $m }
function Write-Ok    { param([string]$m) Write-Log "OK"    $m }

# -----------------------------------------------------------------------------
# Resolve credentials from environment or .env file
# -----------------------------------------------------------------------------
function Get-DbPassword {
    # Priority: environment variable > .env file
    $pwd = $env:DB_ROOT_PASSWORD
    if ($pwd) {
        Write-Info "Using DB_ROOT_PASSWORD from environment variable."
        return $pwd
    }

    # Try loading from .env in the repo root
    $envFile = Join-Path $PSScriptRoot "..\..\..\.env"  # scripts/backup/ -> repo root
    if (-not (Test-Path $envFile)) {
        $envFile = Join-Path (Get-Location) ".env"
    }

    if (Test-Path $envFile) {
        Write-Info "Loading credentials from: $envFile"
        Get-Content $envFile | ForEach-Object {
            if ($_ -match "^DB_ROOT_PASSWORD=(.+)$") {
                return $Matches[1].Trim()
            }
        }
    }

    Write-Err "DB_ROOT_PASSWORD not set in environment or .env file."
    Write-Err "Export DB_ROOT_PASSWORD=<password> before running this script."
    exit 1
}

# -----------------------------------------------------------------------------
# Verify Docker and container availability
# -----------------------------------------------------------------------------
function Assert-ContainerRunning {
    param([string]$Name)

    $status = docker inspect --format '{{.State.Status}}' $Name 2>$null
    if ($LASTEXITCODE -ne 0 -or $status -ne "running") {
        Write-Err "Container '$Name' is not running (status: $status)."
        Write-Err "Start the platform with: docker compose up -d"
        exit 1
    }
    Write-Info "Container '$Name' is running."
}

# -----------------------------------------------------------------------------
# List of databases to back up
# -----------------------------------------------------------------------------
$VIBECHECK_DATABASES = @(
    "vibecheck_auth",
    "vibecheck_movie",
    "vibecheck_theatre",
    "vibecheck_show",
    "vibecheck_booking",
    "vibecheck_payment",
    "vibecheck_notification"
)

# -----------------------------------------------------------------------------
# ACTION: backup
# -----------------------------------------------------------------------------
function Invoke-Backup {
    $dbPassword = Get-DbPassword
    Assert-ContainerRunning $ContainerName

    # Ensure backup directory exists
    $resolvedBackupDir = [System.IO.Path]::GetFullPath($BackupDir)
    if (-not (Test-Path $resolvedBackupDir)) {
        New-Item -ItemType Directory -Force -Path $resolvedBackupDir | Out-Null
        Write-Info "Created backup directory: $resolvedBackupDir"
    }

    $timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
    $backupFile = Join-Path $resolvedBackupDir "vibecheck_backup_$timestamp.sql.gz"
    $dbList = $VIBECHECK_DATABASES -join " "

    Write-Info "Starting MySQL backup..."
    Write-Info "  Databases : $dbList"
    Write-Info "  Output    : $backupFile"
    Write-Info "  Container : $ContainerName"

    # Execute mysqldump inside container, pipe through gzip, write to host
    # Using --single-transaction for InnoDB consistency (no table locks)
    # Using --routines --events for complete schema backup
    # Password passed via MYSQL_PWD environment variable to avoid shell history exposure
    $dockerArgs = @(
        "exec", "-i",
        "-e", "MYSQL_PWD=$dbPassword",
        $ContainerName,
        "mysqldump",
        "-uroot",
        "--single-transaction",
        "--routines",
        "--events",
        "--databases"
    ) + $VIBECHECK_DATABASES

    Write-Info "Executing mysqldump (this may take a moment)..."

    # Run dump and compress
    & docker @dockerArgs | & gzip | Set-Content -Path $backupFile -AsByteStream
    $dumpExitCode = $LASTEXITCODE

    if ($dumpExitCode -ne 0) {
        Write-Err "mysqldump failed with exit code $dumpExitCode."
        # Remove potentially corrupt partial file
        if (Test-Path $backupFile) {
            Remove-Item $backupFile -Force
            Write-Warn "Partial backup file removed."
        }
        exit 1
    }

    # Validate the backup was actually created and has content
    if (-not (Test-Path $backupFile)) {
        Write-Err "Backup file was not created: $backupFile"
        exit 1
    }

    $fileInfo = Get-Item $backupFile
    if ($fileInfo.Length -lt 1024) {
        Write-Err "Backup file is suspiciously small ($($fileInfo.Length) bytes). Backup may be empty or corrupt."
        Remove-Item $backupFile -Force
        exit 1
    }

    Write-Ok "Backup completed successfully."
    Write-Ok "  File : $backupFile"
    Write-Ok "  Size : $([math]::Round($fileInfo.Length / 1MB, 2)) MB"
    Write-Ok "  Time : $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"

    return $backupFile
}

# -----------------------------------------------------------------------------
# ACTION: list
# -----------------------------------------------------------------------------
function Invoke-List {
    $resolvedBackupDir = [System.IO.Path]::GetFullPath($BackupDir)
    if (-not (Test-Path $resolvedBackupDir)) {
        Write-Warn "Backup directory does not exist: $resolvedBackupDir"
        Write-Warn "No backups have been taken yet."
        return
    }

    $files = Get-ChildItem -Path $resolvedBackupDir -Filter "vibecheck_backup_*.sql.gz" |
             Sort-Object LastWriteTime -Descending

    if ($files.Count -eq 0) {
        Write-Warn "No backup files found in: $resolvedBackupDir"
        return
    }

    Write-Info "Found $($files.Count) backup(s) in: $resolvedBackupDir"
    Write-Host ""
    Write-Host ("  {0,-45} {1,12} {2,-24}" -f "Filename", "Size (MB)", "Created") -ForegroundColor White
    Write-Host ("  " + "-" * 85) -ForegroundColor Gray

    foreach ($f in $files) {
        $sizeMB = [math]::Round($f.Length / 1MB, 2)
        Write-Host ("  {0,-45} {1,12} {2,-24}" -f $f.Name, $sizeMB, $f.LastWriteTime.ToString("yyyy-MM-dd HH:mm:ss"))
    }
    Write-Host ""
}

# -----------------------------------------------------------------------------
# ACTION: cleanup
# -----------------------------------------------------------------------------
function Invoke-Cleanup {
    $resolvedBackupDir = [System.IO.Path]::GetFullPath($BackupDir)
    if (-not (Test-Path $resolvedBackupDir)) {
        Write-Info "Backup directory does not exist. Nothing to clean."
        return
    }

    $cutoff = (Get-Date).AddDays(-$RetentionDays)
    Write-Info "Removing backups older than $RetentionDays days (before $($cutoff.ToString('yyyy-MM-dd HH:mm:ss')))..."

    $oldFiles = Get-ChildItem -Path $resolvedBackupDir -Filter "vibecheck_backup_*.sql.gz" |
                Where-Object { $_.LastWriteTime -lt $cutoff }

    if ($oldFiles.Count -eq 0) {
        Write-Info "No backups older than $RetentionDays days. Nothing removed."
        return
    }

    foreach ($f in $oldFiles) {
        Write-Warn "  Removing: $($f.Name) (age: $([math]::Round((Get-Date - $f.LastWriteTime).TotalDays, 1)) days)"
        Remove-Item $f.FullName -Force
    }

    Write-Ok "Cleanup complete. Removed $($oldFiles.Count) file(s)."
}

# -----------------------------------------------------------------------------
# Main entry point
# -----------------------------------------------------------------------------
Write-Info "VibeCheck MySQL Backup - Action: $Action"
Write-Info "Backup directory: $BackupDir"
Write-Info "Retention: $RetentionDays days"

switch ($Action) {
    "backup"  { Invoke-Backup }
    "list"    { Invoke-List }
    "cleanup" { Invoke-Cleanup }
}

exit 0
