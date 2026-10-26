$paths = @(
    "C:\Program Files\Docker\Docker\resources\bin\docker.exe",
    "C:\Program Files\Docker\Docker\Docker Desktop.exe",
    "C:\Program Files\Docker\cli-plugins\docker-compose.exe"
)
foreach ($p in $paths) {
    $exists = Test-Path -LiteralPath $p
    Write-Host "$p -> $exists"
}

$services = Get-Service -Name "*docker*" -ErrorAction SilentlyContinue
if ($services) {
    $services | Format-Table -AutoSize
} else {
    Write-Host "No Windows services matching *docker* found."
}

$wsl = Get-Command wsl -ErrorAction SilentlyContinue
if ($wsl) {
    Write-Host "WSL found at $($wsl.Source)"
    wsl --list --verbose
} else {
    Write-Host "WSL not found."
}
