package com.shop.qa;

import com.shop.qa.core.DriverFactory;
import com.shop.qa.experiments.ExperimentApiClient;
import com.shop.qa.experiments.ExperimentContext;
import com.shop.qa.experiments.ExperimentResolver;
import com.shop.qa.pages.pdp.ProductDetailPage;
import com.shop.qa.support.TestContextHolder;
import org.openqa.selenium.WebDriver;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;

import java.util.Map;

/**
 * Shared lifecycle for PDP tests. Driver is per-method (we run parallel=methods), and the
 * experiment context is rebuilt for each test from the variants that test pins.
 *
 * The openPdp(...) helper is the important bit - it encodes the deterministic-variant
 * recipe once so individual tests stay short and declarative:
 *
 *     ProductDetailPage pdp = openPdp("SKU-123", Map.of(CTA_PLACEMENT, "sticky"));
 *
 * reads as "open this product with the sticky CTA pinned", which is exactly what a test
 * author wants to think about.
 */
public abstract class BaseTest {

    protected WebDriver driver;
    protected ExperimentResolver resolver;

    private String baseUrl;

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        baseUrl = System.getProperty("baseUrl", "https://shop.example.com");
        driver = DriverFactory.create();
        resolver = new ExperimentResolver(new ExperimentApiClient(baseUrl));
    }

    /**
     * Opens a PDP with the given variants pinned, returns an experiment-aware page.
     *
     * @param productId product/SKU to open
     * @param overrides experimentKey -> variant to force; pass Map.of() for default bucketing
     */
    protected ProductDetailPage openPdp(String productId, Map<String, String> overrides) {
        // 1. land on the domain so cookies stick, 2. pin variants, 3. go to the PDP
        driver.get(baseUrl);
        String visitorId = resolver.pin(driver, overrides);
        driver.get(baseUrl + "/p/" + productId);

        // 4. confirm what we actually got from the decision API and publish the context
        ExperimentContext context = resolver.resolve(visitorId, overrides);
        TestContextHolder.set(context);

        return new ProductDetailPage(driver, context);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        TestContextHolder.clear();
        if (driver != null) {
            driver.quit();
        }
    }
}
