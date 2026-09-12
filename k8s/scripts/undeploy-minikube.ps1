# ==============================================================================
# VibeCheck Platform - Minikube Teardown Script
# ==============================================================================
$ErrorActionPreference = "Continue"

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host " UNINSTALLING VIBECHECK HELM RELEASE" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

Write-Host "Uninstalling Helm release 'vibecheck'..." -ForegroundColor Yellow
helm uninstall vibecheck -n vibecheck

Write-Host "Deleting namespace 'vibecheck'..." -ForegroundColor Yellow
kubectl delete namespace vibecheck --timeout=60s

Write-Host "`nTeardown complete." -ForegroundColor Green
