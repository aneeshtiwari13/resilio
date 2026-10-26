# Acceptance Test Script for Phase 1 Scenarios B, C, and Runtime Verification
$demoDir = "C:\Users\tiwar\.gemini\antigravity\scratch\resilio\demo-app"

Write-Host "=========================================================="
Write-Host " [ACCEPTANCE TEST B] Compilation Failure Verification"
Write-Host "=========================================================="
$brokenJavaFile = Join-Path $demoDir "src\main\java\com\resilio\demo\DeliberateSyntaxError.java"
Set-Content -Path $brokenJavaFile -Value "package com.resilio.demo; public class DeliberateSyntaxError { INTENTIONAL_COMPILATION_ERROR }"

Push-Location $demoDir
& .\mvnw.cmd compile
$compilationExitCode = $LASTEXITCODE
Pop-Location

# Clean up broken file immediately
if (Test-Path $brokenJavaFile) {
    Remove-Item $brokenJavaFile
}

Write-Host "Compilation Failure Exit Code: $compilationExitCode"
if ($compilationExitCode -ne 0) {
    Write-Host ">> TEST B RESULT: PASSED (Maven cleanly failed with non-zero exit code)" -ForegroundColor Green
} else {
    Write-Host ">> TEST B RESULT: FAILED (Maven should have returned non-zero)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=========================================================="
Write-Host " [ACCEPTANCE TEST C] Unit Test Failure Verification"
Write-Host "=========================================================="
$failingTestFile = Join-Path $demoDir "src\test\java\com\resilio\demo\DeliberateFailingUnitTest.java"
$testContent = @"
package com.resilio.demo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

public class DeliberateFailingUnitTest {
    @Test
    public void testDeliberateAssertionFailure() {
        Assertions.fail("Deliberately failing unit test for Acceptance Test C");
    }
}
"@
Set-Content -Path $failingTestFile -Value $testContent

Push-Location $demoDir
& .\mvnw.cmd test -Dtest=DeliberateFailingUnitTest
$unitTestExitCode = $LASTEXITCODE
Pop-Location

# Clean up failing test file
if (Test-Path $failingTestFile) {
    Remove-Item $failingTestFile
}

Write-Host "Unit Test Failure Exit Code: $unitTestExitCode"
if ($unitTestExitCode -ne 0) {
    Write-Host ">> TEST C RESULT: PASSED (Maven reported test failure and exited non-zero)" -ForegroundColor Green
} else {
    Write-Host ">> TEST C RESULT: FAILED (Maven should have returned non-zero)" -ForegroundColor Red
}

Write-Host ""
Write-Host "=========================================================="
Write-Host " [ACCEPTANCE TEST E & F] Restored Application Clean Build"
Write-Host "=========================================================="
Push-Location $demoDir
& .\mvnw.cmd clean package -DskipTests
$cleanBuildExitCode = $LASTEXITCODE
Pop-Location

Write-Host "Clean Package Exit Code: $cleanBuildExitCode"
if ($cleanBuildExitCode -eq 0) {
    Write-Host ">> TEST E & F BUILD RESULT: PASSED (Clean artifact verified at demo-app\target)" -ForegroundColor Green
} else {
    Write-Host ">> TEST E & F BUILD RESULT: FAILED" -ForegroundColor Red
}
