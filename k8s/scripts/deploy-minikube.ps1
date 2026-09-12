# ==============================================================================
# VibeCheck Platform - Minikube Deployment Automation Script
# ==============================================================================
param(
    [switch]$SkipImageBuild,
    [int]$Cpus = 4,
    [int]$Memory = 8192
)

$ErrorActionPreference = "Stop"

Write-Host "========================================================" -ForegroundColor Cyan
Write-Host " VIBECHECK KUBERNETES DEPLOYMENT ON MINIKUBE" -ForegroundColor Cyan
Write-Host "========================================================" -ForegroundColor Cyan

# 1. Check Minikube Status
Write-Host "`n[Step 1] Checking Minikube cluster status..." -ForegroundColor Yellow
$status = minikube status --format "{{.Host}}" 2>$null
if ($status -ne "Running") {
    Write-Host "Starting Minikube with $Cpus CPUs and ${Memory}MB Memory..." -ForegroundColor Yellow
    minikube start --driver=docker --cpus=$Cpus --memory=$Memory
} else {
    Write-Host "Minikube is already Running." -ForegroundColor Green
}

# 2. Enable Required Addons
Write-Host "`n[Step 2] Enabling Ingress and Metrics Server addons..." -ForegroundColor Yellow
minikube addons enable ingress
minikube addons enable metrics-server

# 3. Build & Load Local Docker Images into Minikube
if (-not $SkipImageBuild) {
    Write-Host "`n[Step 3] Loading local Docker images into Minikube..." -ForegroundColor Yellow
    $services = @(
        "vibecheck-gateway",
        "vibecheck-auth",
        "vibecheck-movie",
        "vibecheck-theatre",
        "vibecheck-show",
        "vibecheck-booking",
        "vibecheck-payment",
        "vibecheck-notification"
    )

    foreach ($svc in $services) {
        Write-Host "Checking image $svc:latest in Minikube..." -ForegroundColor Cyan
        $imageExists = minikube image ls --format "{{.RepoTags}}" | Select-String "$svc:latest"
        if (-not $imageExists) {
            Write-Host "Loading $svc:latest into Minikube cache..." -ForegroundColor Yellow
            minikube image load "$svc:latest"
        } else {
            Write-Host "$svc:latest is already available in Minikube." -ForegroundColor Green
        }
    }
} else {
    Write-Host "`n[Step 3] Skipping image build/load as requested." -ForegroundColor Gray
}

# 4. Deploy Helm Chart
Write-Host "`n[Step 4] Deploying VibeCheck Helm chart..." -ForegroundColor Yellow
helm upgrade --install vibecheck ./k8s/helm/vibecheck `
    -f ./k8s/helm/vibecheck/values-minikube.yaml `
    --create-namespace `
    -n vibecheck

# 5. Wait for Stateful Infrastructure Rollouts
Write-Host "`n[Step 5] Waiting for infrastructure pods to become Ready..." -ForegroundColor Yellow
Write-Host "Waiting for MySQL StatefulSet..." -ForegroundColor Cyan
kubectl rollout status statefulset/mysql -n vibecheck --timeout=180s

Write-Host "Waiting for Redis StatefulSet..." -ForegroundColor Cyan
kubectl rollout status statefulset/redis -n vibecheck --timeout=120s

Write-Host "Waiting for Zookeeper..." -ForegroundColor Cyan
kubectl rollout status deployment/zookeeper -n vibecheck --timeout=120s

Write-Host "Waiting for Kafka StatefulSet..." -ForegroundColor Cyan
kubectl rollout status statefulset/kafka -n vibecheck --timeout=180s

# 6. Wait for Core Microservices Rollouts
Write-Host "`n[Step 6] Waiting for microservices to become Ready..." -ForegroundColor Yellow
$deployments = @(
    "auth-service",
    "movie-service",
    "theatre-service",
    "show-service",
    "booking-service",
    "payment-service",
    "notification-service",
    "gateway-service"
)

foreach ($dep in $deployments) {
    Write-Host "Waiting for $dep..." -ForegroundColor Cyan
    kubectl rollout status deployment/$dep -n vibecheck --timeout=240s
}

# 7. Print Deployment Summary
Write-Host "`n========================================================" -ForegroundColor Green
Write-Host " DEPLOYMENT COMPLETE" -ForegroundColor Green
Write-Host "========================================================" -ForegroundColor Green
kubectl get pods,svc,ingress -n vibecheck

$minikubeIp = minikube ip
Write-Host "`nMinikube IP: $minikubeIp" -ForegroundColor Cyan
Write-Host "To access via Ingress, add to your hosts file (C:\Windows\System32\drivers\etc\hosts):" -ForegroundColor Yellow
Write-Host "$minikubeIp  vibecheck.local" -ForegroundColor White
Write-Host "`nOr use port-forwarding to access Gateway directly:" -ForegroundColor Yellow
Write-Host "kubectl port-forward svc/gateway-service 8079:8079 -n vibecheck" -ForegroundColor White
