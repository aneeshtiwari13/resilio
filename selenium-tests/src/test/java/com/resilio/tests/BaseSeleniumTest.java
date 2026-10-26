package com.resilio.tests;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.openqa.selenium.support.ui.WebDriverWait;

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
    public void setupDriver() {
        targetBaseUrl = System.getProperty("target.base.url",
                System.getenv().getOrDefault("TARGET_BASE_URL", "http://localhost:8080"));
        seleniumGridUrl = System.getProperty("selenium.grid.url",
                System.getenv().getOrDefault("SELENIUM_GRID_URL", "http://localhost:4444/wd/hub"));

        String preferredDriver = System.getProperty("selenium.driver", "auto"); // remote, edge, chrome, or auto

        // Strategy 1: Remote Grid (Default in Jenkins / Docker Compose)
        if ("remote".equalsIgnoreCase(preferredDriver) || "auto".equalsIgnoreCase(preferredDriver)) {
            try {
                ChromeOptions options = new ChromeOptions();
                options.addArguments("--headless=new", "--disable-gpu", "--no-sandbox", "--disable-dev-shm-usage", "--window-size=1920,1080");
                driver = new RemoteWebDriver(new URL(seleniumGridUrl), options);
                System.out.println("[SELENIUM] Connected to Remote Selenium Grid at: " + seleniumGridUrl);
            } catch (Exception remoteEx) {
                if ("remote".equalsIgnoreCase(preferredDriver)) {
                    throw new RuntimeException("SELENIUM_INFRASTRUCTURE_FAILURE: Remote grid unavailable at "
                            + seleniumGridUrl + ". " + remoteEx.getMessage(), remoteEx);
                }
                // In auto mode, attempt local fallback
                System.out.println("[SELENIUM] Remote Grid not reachable at " + seleniumGridUrl + ". Attempting local browser fallback...");
            }
        }

        // Strategy 2: Local Edge / Chrome Fallback (for local Windows developer machine without Docker)
        if (driver == null) {
            try {
                // Try local Edge first on Windows
                EdgeOptions edgeOptions = new EdgeOptions();
                edgeOptions.addArguments("--headless=new", "--disable-gpu", "--no-sandbox", "--window-size=1920,1080");
                driver = new EdgeDriver(edgeOptions);
                System.out.println("[SELENIUM] Initialized local Microsoft Edge browser in headless mode.");
            } catch (Exception edgeEx) {
                try {
                    // Try local Chrome
                    ChromeOptions chromeOptions = new ChromeOptions();
                    chromeOptions.addArguments("--headless=new", "--disable-gpu", "--no-sandbox", "--window-size=1920,1080");
                    driver = new ChromeDriver(chromeOptions);
                    System.out.println("[SELENIUM] Initialized local Google Chrome browser in headless mode.");
                } catch (Exception chromeEx) {
                    throw new RuntimeException("SELENIUM_INFRASTRUCTURE_FAILURE: Neither Remote Grid (" + seleniumGridUrl
                            + ") nor local browsers (Edge/Chrome) could be initialized. Error: " + edgeEx.getMessage(), edgeEx);
                }
            }
        }

        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(3));
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
