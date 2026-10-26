package com.resilio.tests;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;

public class SeleniumFailureWatcher implements TestWatcher {

    @Override
    public void testFailed(ExtensionContext context, Throwable cause) {
        Object testInstance = context.getRequiredTestInstance();
        if (testInstance instanceof BaseSeleniumTest baseTest) {
            WebDriver driver = baseTest.getDriver();
            if (driver instanceof TakesScreenshot screenshotDriver) {
                try {
                    byte[] pngBytes = screenshotDriver.getScreenshotAs(OutputType.BYTES);
                    File dir = new File("target/screenshots");
                    if (!dir.exists()) {
                        dir.mkdirs();
                    }
                    String testName = context.getTestMethod().map(m -> m.getName()).orElse("unknown_test");
                    File file = new File(dir, "FAIL_" + testName + "_" + System.currentTimeMillis() + ".png");
                    try (FileOutputStream fos = new FileOutputStream(file)) {
                        fos.write(pngBytes);
                    }
                    System.err.println("[SELENIUM_DIAGNOSTIC] Screenshot saved on test failure to: " + file.getAbsolutePath());
                } catch (IOException e) {
                    System.err.println("[SELENIUM_DIAGNOSTIC] Could not capture screenshot: " + e.getMessage());
                }
            }
        }
    }
}
