package com.shop.qa.core;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

/**
 * Creates drivers. Local runs use Selenium Manager (no driver binaries to manage);
 * CI points REMOTE_URL at a Selenium Grid / cloud provider so the same code runs
 * headless across a browser matrix.
 *
 * One driver per test thread - never share. testng.xml runs parallel=methods, so each
 * @Test gets its own driver via ThreadLocal in BaseTest.
 */
public final class DriverFactory {

    private DriverFactory() {
    }

    public static WebDriver create() {
        String browser = System.getProperty("browser", "chrome").toLowerCase();
        String remoteUrl = System.getProperty("remoteUrl", System.getenv("REMOTE_URL"));

        WebDriver driver = (remoteUrl != null && !remoteUrl.isBlank())
                ? remote(browser, remoteUrl)
                : local(browser);

        // No implicit waits. They fight with explicit waits and hide real timing bugs.
        // All waiting goes through WaitUtils with explicit conditions.
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        driver.manage().window().maximize();
        return driver;
    }

    private static WebDriver local(String browser) {
        switch (browser) {
            case "firefox":
                return new org.openqa.selenium.firefox.FirefoxDriver(firefoxOptions());
            case "chrome":
            default:
                return new org.openqa.selenium.chrome.ChromeDriver(chromeOptions());
        }
    }

    private static WebDriver remote(String browser, String remoteUrl) {
        try {
            return new RemoteWebDriver(new URL(remoteUrl),
                    "firefox".equals(browser) ? firefoxOptions() : chromeOptions());
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Bad remoteUrl: " + remoteUrl, e);
        }
    }

    private static ChromeOptions chromeOptions() {
        ChromeOptions opts = new ChromeOptions();
        if (Boolean.parseBoolean(System.getProperty("headless", "true"))) {
            opts.addArguments("--headless=new");
        }
        opts.addArguments("--window-size=1440,900", "--no-sandbox", "--disable-dev-shm-usage");
        return opts;
    }

    private static FirefoxOptions firefoxOptions() {
        FirefoxOptions opts = new FirefoxOptions();
        if (Boolean.parseBoolean(System.getProperty("headless", "true"))) {
            opts.addArguments("-headless");
        }
        return opts;
    }
}
