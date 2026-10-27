# ==============================================================================
# RESILIO - Service Terminator
# Safely stops all processes listening on port 8080 and 8090
# ==============================================================================

Write-Host "Stopping Resilio processes on ports 8080 and 8090..." -ForegroundColor Yellow

$ports = @(8080, 8090)
$stopped = 0

foreach ($port in $ports) {
    $connections = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($connections) {
        $pids = $connections | Select-Object -ExpandProperty OwningProcess -Unique
        foreach ($procId in $pids) {
            try {
                Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
                Write-Host "Stopped process $procId on port $port" -ForegroundColor Green
                $stopped++
            } catch {
                Write-Host "Could not terminate process $procId" -ForegroundColor Red
            }
        }
    }
}

if ($stopped -eq 0) {
    Write-Host "No active processes found on ports 8080 or 8090." -ForegroundColor DarkGray
} else {
    Write-Host "Clean shutdown complete." -ForegroundColor Green
}
