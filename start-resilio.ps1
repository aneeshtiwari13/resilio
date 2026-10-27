# ==============================================================================
# RESILIO - One-Click Demonstration Launcher
# Launches both Target Microservice (8080) and Control Center (8090)
# ==============================================================================

$ResilioRoot = $PSScriptRoot

Write-Host "==========================================================" -ForegroundColor Cyan
Write-Host "     RESILIO: Self-Healing CI/CD Platform Launcher        " -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor Cyan

# 1. Start Demo App (Target Workload) on port 8080 in separate window
Write-Host "`n[1/2] Starting Target Demo App on port 8080..." -ForegroundColor Green
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$ResilioRoot\demo-app'; `$Host.UI.RawUI.WindowTitle = 'RESILIO: Target Demo Microservice (8080)'; Write-Host '>>> Target Microservice starting on http://localhost:8080...' -ForegroundColor Green; .\mvnw.cmd spring-boot:run"

# Wait 3 seconds before starting backend
Start-Sleep -Seconds 3

# 2. Start Resilio Backend (Control Center) on port 8090 in separate window
Write-Host "[2/2] Starting Resilio Control Center on port 8090..." -ForegroundColor Yellow
Start-Process powershell -ArgumentList "-NoExit", "-Command", "cd '$ResilioRoot\backend'; `$Host.UI.RawUI.WindowTitle = 'RESILIO: Control Center (8090)'; Write-Host '>>> Resilio Control Center starting on http://localhost:8090...' -ForegroundColor Yellow; .\mvnw.cmd spring-boot:run"

Write-Host "`nSUCCESS! Both services are launching in their own dedicated windows." -ForegroundColor Green
Write-Host "Dashboard URL:   http://localhost:8090" -ForegroundColor Cyan
Write-Host "Target App URL:  http://localhost:8080" -ForegroundColor Cyan
Write-Host "`nTo stop all services later, run: .\stop-resilio.ps1`n" -ForegroundColor DarkGray
