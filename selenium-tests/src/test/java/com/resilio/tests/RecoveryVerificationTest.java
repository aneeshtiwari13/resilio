package com.resilio.tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Post-Recovery Verification Test Suite.
 * Runs against the public route after rollback to verify restored version.
 */
public class RecoveryVerificationTest extends BaseSeleniumTest {

    @Test
    @DisplayName("Post-Rollback Verification: Public route active, expected restored version reports, and workflow passes")
    public void testPostRollbackVerification() {
        String expectedRestoredVersion = System.getProperty("expected.version", System.getenv().getOrDefault("EXPECTED_VERSION", "1.0.0"));

        // 1. Public route is accessible
        driver.get(targetBaseUrl + "/");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("main-nav")));

        // 2. Expected previous version is active
        WebElement versionElem = driver.findElement(By.id("app-version"));
        Assertions.assertEquals(expectedRestoredVersion, versionElem.getText().trim(),
                "Restored release must match expected known-good baseline version: " + expectedRestoredVersion);

        // 3. Actuator health returns UP
        driver.get(targetBaseUrl + "/actuator/health");
        WebElement body = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        Assertions.assertTrue(body.getText().contains("\"status\":\"UP\""),
                "Restored application health endpoint must respond UP");

        // 4. Functional workflow verification passes on restored application
        driver.get(targetBaseUrl + "/");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("workflow-form")));

        WebElement input = driver.findElement(By.id("payload-input"));
        WebElement submitBtn = driver.findElement(By.id("validate-btn"));

        input.clear();
        input.sendKeys("POST-RECOVERY-SMOKE-VERIFY");
        submitBtn.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("result-message")));
        WebElement resultBox = driver.findElement(By.id("result-message"));
        Assertions.assertEquals("success", resultBox.getAttribute("data-status"),
                "Restored application must successfully execute user workflows");
    }
}
