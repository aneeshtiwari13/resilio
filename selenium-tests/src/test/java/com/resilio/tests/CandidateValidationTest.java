package com.resilio.tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Pre-Promotion Candidate Validation Test Suite.
 * Runs against the candidate container before traffic activation.
 */
public class CandidateValidationTest extends BaseSeleniumTest {

    @Test
    @DisplayName("Pre-Promotion Gate: Candidate loads, health responds UP, version matches, and workflow passes")
    public void testCandidateValidationGate() {
        String expectedVersion = System.getProperty("expected.version", System.getenv().getOrDefault("EXPECTED_VERSION", ""));

        // 1. Candidate Home Page Loads
        driver.get(targetBaseUrl + "/");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("main-nav")));

        WebElement appStatus = driver.findElement(By.id("app-status"));
        Assertions.assertTrue(appStatus.getText().contains("Healthy"), "Candidate status must indicate Healthy");

        // 2. Candidate Version Verification
        if (!expectedVersion.isBlank()) {
            WebElement versionElem = driver.findElement(By.id("app-version"));
            Assertions.assertEquals(expectedVersion, versionElem.getText().trim(),
                    "Candidate version must match expected release version: " + expectedVersion);
        }

        // 3. Candidate Readiness Actuator Check
        driver.get(targetBaseUrl + "/actuator/health");
        WebElement body = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        Assertions.assertTrue(body.getText().contains("\"status\":\"UP\""),
                "Candidate actuator health probe must return UP");

        // 4. Representative User Workflow Verification
        driver.get(targetBaseUrl + "/");
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("workflow-form")));

        WebElement input = driver.findElement(By.id("payload-input"));
        WebElement submitBtn = driver.findElement(By.id("validate-btn"));

        input.clear();
        input.sendKeys("CANDIDATE-PRE-PROMOTION-CHECK");
        submitBtn.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("result-message")));
        WebElement resultBox = driver.findElement(By.id("result-message"));
        Assertions.assertEquals("success", resultBox.getAttribute("data-status"),
                "Candidate workflow verification must pass prior to production traffic switch");
    }
}
