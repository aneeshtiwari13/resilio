package com.resilio.tests;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class HomePageSmokeTest extends BaseSeleniumTest {

    @Test
    public void testHomePageLoadsSuccessfully() {
        driver.get(targetBaseUrl + "/");

        // Explicit wait for title and main card
        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("active-env-badge")));

        WebElement versionElem = driver.findElement(By.id("app-version"));
        Assertions.assertNotNull(versionElem.getText());
        Assertions.assertFalse(versionElem.getText().isBlank(), "Application version must be visible on UI");

        WebElement envBadge = driver.findElement(By.id("active-env-badge"));
        Assertions.assertTrue(envBadge.getText().contains("ENVIRONMENT"), "Environment badge must be present");
    }

    @Test
    public void testValidAuthenticationWorkflow() {
        driver.get(targetBaseUrl + "/");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("auth-workflow-form")));

        WebElement usernameInput = driver.findElement(By.id("username"));
        WebElement passwordInput = driver.findElement(By.id("password"));
        WebElement submitButton = driver.findElement(By.id("submit-btn"));

        usernameInput.sendKeys("demo-operator");
        passwordInput.sendKeys("resilio2026");
        submitButton.click();

        // Await confirmation message
        WebElement successMsg = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-success")));
        Assertions.assertTrue(successMsg.getText().contains("Functional verification successful"),
                "Expected successful authentication acknowledgment");
    }

    @Test
    public void testInvalidAuthenticationRejection() {
        driver.get(targetBaseUrl + "/");

        wait.until(ExpectedConditions.presenceOfElementLocated(By.id("auth-workflow-form")));

        WebElement usernameInput = driver.findElement(By.id("username"));
        WebElement passwordInput = driver.findElement(By.id("password"));
        WebElement submitButton = driver.findElement(By.id("submit-btn"));

        usernameInput.sendKeys("invalid-user");
        passwordInput.sendKeys("wrong-pass");
        submitButton.click();

        WebElement failMsg = wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("login-failure")));
        Assertions.assertTrue(failMsg.getText().contains("Invalid credentials"),
                "Expected invalid credentials rejection alert");
    }
}
