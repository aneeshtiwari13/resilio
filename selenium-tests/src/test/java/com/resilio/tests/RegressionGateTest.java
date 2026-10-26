package com.resilio.tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

/**
 * Gate test that fails if functional regression is detected on the candidate container.
 * Verified in Phase 1 Acceptance Test D.
 */
public class RegressionGateTest extends BaseSeleniumTest {

    @Test
    @DisplayName("Candidate Regression Gate: Verifies critical business processing")
    public void testCandidateRegressionGate() {
        driver.get(targetBaseUrl + "/");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("workflow-form")));
        WebElement input = driver.findElement(By.id("payload-input"));
        WebElement submitBtn = driver.findElement(By.id("validate-btn"));

        input.clear();
        input.sendKeys("REGRESSION-GATE-CHECK");
        submitBtn.click();

        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("result-message")));
        WebElement resultBox = driver.findElement(By.id("result-message"));

        // Must succeed: if regression is present, data-status will be 'error' and test will fail
        Assertions.assertEquals("success", resultBox.getAttribute("data-status"),
                "GATE FAILURE: Functional regression detected on candidate application!");
    }
}
