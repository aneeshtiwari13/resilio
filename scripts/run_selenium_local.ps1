$env:TARGET_BASE_URL = "http://localhost:8080"
$env:SELENIUM_GRID_URL = "http://localhost:4444/wd/hub"

Push-Location "C:\Users\tiwar\.gemini\antigravity\scratch\resilio\selenium-tests"
& .\mvnw.cmd test -Dtest=ResilioFunctionalTest
$code = $LASTEXITCODE
Pop-Location

Write-Host "Selenium test suite exit code: $code"
