package com.resilio.tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class ResilioFunctionalTest extends BaseSeleniumTest {

    @Test
    @DisplayName("Scenario 1: Home page loads successfully with navigation and layout")
    public void testHomePageLoads() {
        driver.get(targetBaseUrl + "/");

        // Verify navbar brand and app title
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("main-nav")));
        WebElement titleElem = driver.findElement(By.id("app-title"));
        Assertions.assertTrue(titleElem.getText().contains("Resilio Demo Application"),
                "Home page must display application name 'Resilio Demo Application'");

        // Verify app description and status badge
        WebElement statusElem = driver.findElement(By.id("app-status"));
        Assertions.assertTrue(statusElem.getText().contains("Healthy"),
                "Application status badge must indicate 'Healthy'");

        WebElement descElem = driver.findElement(By.id("app-description"));
        Assertions.assertTrue(descElem.getText().contains("tested, containerized, and deployed by Resilio"),
                "Application description must explain Resilio pipeline context");
    }

    @Test
    @DisplayName("Scenario 2: Application version and metadata displayed correctly")
    public void testApplicationVersionDisplayedCorrectly() {
        driver.get(targetBaseUrl + "/");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("app-version")));
        WebElement versionElem = driver.findElement(By.id("app-version"));
        String versionText = versionElem.getText();
        Assertions.assertNotNull(versionText, "Version element must not be null");
        Assertions.assertFalse(versionText.isBlank(), "Version text must not be empty");

        WebElement commitElem = driver.findElement(By.id("commit-sha"));
        Assertions.assertFalse(commitElem.getText().isBlank(), "Commit SHA must not be empty");
    }

    @Test
    @DisplayName("Scenario 3: Actuator health endpoint is available and healthy")
    public void testHealthEndpointAvailable() {
        driver.get(targetBaseUrl + "/actuator/health");

        // Validate JSON content in browser body
        WebElement body = wait.until(ExpectedConditions.presenceOfElementLocated(By.tagName("body")));
        Assertions.assertTrue(body.getText().contains("\"status\":\"UP\""),
                "Actuator health endpoint must respond with status UP");
    }

    @Test
    @DisplayName("Scenario 4: Valid form submission succeeds")
    public void testValidFormSubmissionSucceeds() {
        driver.get(targetBaseUrl + "/");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("workflow-form")));
        WebElement input = driver.findElement(By.id("payload-input"));
        WebElement submitBtn = driver.findElement(By.id("validate-btn"));

        input.clear();
        input.sendKeys("RESILIO-TEST-PASS-2026");
        submitBtn.click();

        // Await confirmation message
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("result-message")));
        WebElement resultBox = driver.findElement(By.id("result-message"));
        Assertions.assertEquals("success", resultBox.getAttribute("data-status"),
                "Valid input must result in success state");

        WebElement resultText = driver.findElement(By.id("result-text"));
        Assertions.assertTrue(resultText.getText().contains("Validation Passed"),
                "Result banner must announce validation pass");
    }

    @Test
    @DisplayName("Scenario 5: Invalid form submission is rejected")
    public void testInvalidFormSubmissionRejected() {
        driver.get(targetBaseUrl + "/");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("workflow-form")));
        WebElement input = driver.findElement(By.id("payload-input"));
        WebElement submitBtn = driver.findElement(By.id("validate-btn"));

        // Supply invalid input (< 4 characters)
        input.clear();
        input.sendKeys("ab");
        submitBtn.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("result-message")));
        WebElement resultBox = driver.findElement(By.id("result-message"));
        Assertions.assertEquals("error", resultBox.getAttribute("data-status"),
                "Invalid input must result in error state");

        WebElement resultText = driver.findElement(By.id("result-text"));
        Assertions.assertTrue(resultText.getText().contains("Validation Error"),
                "Result banner must announce validation error rejection");
    }

    @Test
    @DisplayName("Scenario 6: Expected processed result is displayed correctly")
    public void testExpectedResultDisplayed() {
        driver.get(targetBaseUrl + "/");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("workflow-form")));
        WebElement input = driver.findElement(By.id("payload-input"));
        WebElement submitBtn = driver.findElement(By.id("validate-btn"));

        input.clear();
        input.sendKeys("ALPHA-RELEASE");
        submitBtn.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("processed-value")));
        WebElement processedElem = driver.findElement(By.id("processed-value"));
        Assertions.assertEquals("Output: PROCESSED::ALPHA-RELEASE", processedElem.getText().trim(),
                "Output display must show the correctly processed uppercase payload");
    }
}
