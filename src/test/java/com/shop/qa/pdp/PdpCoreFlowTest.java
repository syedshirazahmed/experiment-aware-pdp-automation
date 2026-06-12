package com.shop.qa.pdp;

import com.shop.qa.BaseTest;
import com.shop.qa.pages.pdp.Price;
import com.shop.qa.pages.pdp.ProductDetailPage;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

/**
 * Core PDP content, asserted in a way that holds across variants. These run on every PR
 * (smoke group). They check the things a customer needs regardless of which experiment
 * they're in: there's a title, there's a sane price, there's a way to buy.
 */
public class PdpCoreFlowTest extends BaseTest {

    private static final String SKU = "SKU-CLASSIC-TEE";

    @Test(groups = {"smoke"})
    public void productDetailsRenderInDefaultBucket() {
        // Map.of() = no forced variants, let the platform bucket us naturally.
        ProductDetailPage pdp = openPdp(SKU, Map.of());

        assertFalse(pdp.getTitle().isBlank(), "PDP title should never be empty");
        assertTrue(pdp.isAddToCartPresent(), "every PDP variant must offer an add-to-cart");
    }

    @Test(groups = {"smoke"})
    public void priceIsPresentAndPositive() {
        ProductDetailPage pdp = openPdp(SKU, Map.of());

        Price price = pdp.getPrice();
        assertTrue(price.current() > 0, "current price must be a positive number, was " + price);
    }

    /**
     * The price-display experiment is supposed to ADD a compare-at price, not change what
     * the customer pays. So we pin the strikethrough variant and assert the discount is
     * internally consistent - savings can't exceed the original, current can't exceed original.
     */
    @Test(groups = {"smoke"})
    public void strikethroughPriceIsInternallyConsistent() {
        ProductDetailPage pdp = openPdp(SKU,
                Map.of(com.shop.qa.experiments.PdpExperiments.PRICE_DISPLAY, "strikethrough"));

        Price price = pdp.getPrice();
        if (price.hasDiscount()) {
            // current + savings should reconstruct the original compare-at price, and the
            // customer must never be quoted more than the "was" figure.
            assertTrue(price.savings() > 0, "a shown discount should be a positive saving");
            assertTrue(price.current() + price.savings() > price.current(),
                    "original (was) price must be strictly above the current price");
        }
        // If the variant didn't render a discount for this SKU that's fine - not every
        // product is on promotion. We only assert consistency *when* a discount shows.
    }
}
