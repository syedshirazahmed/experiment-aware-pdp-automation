package com.shop.qa.pdp;

import com.shop.qa.BaseTest;
import com.shop.qa.experiments.PdpExperiments;
import com.shop.qa.pages.pdp.ProductDetailPage;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;

/**
 * Add-to-cart is the money path, so it gets its own coverage across the CTA-placement
 * variants. Note the test body is identical for every variant - the page object hides
 * where the button lives. That's the payoff of the experiment-aware facade: the behaviour
 * we assert ("clicking add-to-cart increments the cart") is written once, not per layout.
 */
public class PdpAddToCartTest extends BaseTest {

    private static final String SKU = "SKU-CLASSIC-TEE";

    @Test(groups = {"smoke"})
    public void addToCartWithInlineCta() {
        addToCartIncrementsBadge(Map.of(PdpExperiments.CTA_PLACEMENT, "control"));
    }

    @Test(groups = {"smoke"})
    public void addToCartWithStickyCta() {
        // Sticky footer overlaps content; AddToCartCta handles the scroll/JS-click. The
        // test doesn't know or care that this variant is harder to click.
        addToCartIncrementsBadge(Map.of(PdpExperiments.CTA_PLACEMENT, "sticky"));
    }

    private void addToCartIncrementsBadge(Map<String, String> variants) {
        ProductDetailPage pdp = openPdp(SKU, variants);

        pdp.addToCart();
        int count = pdp.cartCountAfterAdd();

        assertEquals(count, 1, "cart badge should read 1 after a single add-to-cart");
    }
}
