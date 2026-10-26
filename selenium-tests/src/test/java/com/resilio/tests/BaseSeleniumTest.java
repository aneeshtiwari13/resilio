package com.resilio.tests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

@ExtendWith(SeleniumFailureWatcher.class)
public abstract class BaseSeleniumTest {

    protected WebDriver driver;
    protected WebDriverWait wait;
    protected String targetBaseUrl;
    protected String seleniumGridUrl;

    public WebDriver getDriver() {
        return driver;
    }

    @BeforeEach
    public void setupDriver() throws MalformedURLException {
        targetBaseUrl = System.getProperty("target.base.url",
                System.getenv().getOrDefault("TARGET_BASE_URL", "http://resilio-demo-app:8080"));
        seleniumGridUrl = System.getProperty("selenium.grid.url",
                System.getenv().getOrDefault("SELENIUM_GRID_URL", "http://localhost:4444/wd/hub"));

        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage", "--window-size=1920,1080");

        try {
            driver = new RemoteWebDriver(new URL(seleniumGridUrl), options);
        } catch (Exception e) {
            throw new RuntimeException("SELENIUM_INFRASTRUCTURE_FAILURE: Unable to reach Selenium Chrome container at "
                    + seleniumGridUrl + ". Error: " + e.getMessage(), e);
        }

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(2));
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
    }

    @AfterEach
    public void teardown() {
        if (driver != null) {
            try {
                driver.quit();
            } catch (Exception ignored) {}
        }
    }
}
