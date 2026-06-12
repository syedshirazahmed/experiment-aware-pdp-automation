package com.shop.qa.core;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

/**
 * Explicit waits, condition-based. The golden rule on an experiment-heavy site:
 * never Thread.sleep, and never assume an element is present just because the URL loaded.
 * Variants render different chunks of JS at different times.
 */
public final class WaitUtils {

    private static final Duration DEFAULT = Duration.ofSeconds(10);

    private WaitUtils() {
    }

    public static WebElement waitForElementToBeVisible(WebDriver driver, By locator) {
        return waitForElementToBeVisible(driver, locator, DEFAULT);
    }

    public static WebElement waitForElementToBeVisible(WebDriver driver, By locator, Duration timeout) {
        return new WebDriverWait(driver, timeout)
                .until(ExpectedConditions.visibilityOfElementLocated(locator));
    }

    public static void scrollIntoView(WebDriver driver, WebElement element) {
        ((JavascriptExecutor) driver).executeScript(
                "arguments[0].scrollIntoView();", element);
    }
}
