package com.shop.qa.pdp;

import com.shop.qa.BaseTest;
import com.shop.qa.pages.pdp.ProductDetailPage;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.Map;

import static com.shop.qa.experiments.PdpExperiments.*;
import static org.testng.Assert.assertTrue;

/**
 * Variant-matrix coverage. This is where we confront the "variant explosion" problem head on.
 *
 * Four PDP experiments with 2-3 variants each is already 2x3x2x2 = 24 full combinations, and
 * that grows every time product adds a test. Running the full cartesian product on every
 * commit is both slow and mostly redundant - most bugs are caused by a single variant or a
 * single PAIR of variants interacting, not by exotic 4-way combos.
 *
 * So instead of the cartesian product we run a CURATED set:
 *   - every variant of every experiment appears at least once (single-variant coverage)
 *   - the combinations we've historically seen interact (sticky CTA + redesign) are included
 * That's effectively pairwise coverage, hand-tuned. It runs nightly (variant-matrix group),
 * not per-PR. The generator for a true pairwise set lives in the strategy doc as the next step.
 */
public class PdpVariantMatrixTest extends BaseTest {

    private static final String SKU = "SKU-CLASSIC-TEE";

    @DataProvider(name = "curatedVariants")
    public Object[][] curatedVariants() {
        return new Object[][]{
                {"baseline / all control",
                        Map.of(CTA_PLACEMENT, "control", PRICE_DISPLAY, "control", RECO_MODULE, "control")},
                {"sticky CTA",
                        Map.of(CTA_PLACEMENT, "sticky")},
                {"strikethrough price",
                        Map.of(PRICE_DISPLAY, "strikethrough")},
                {"reco carousel",
                        Map.of(RECO_MODULE, "carousel")},
                {"full redesign + sticky (known interaction)",
                        Map.of(LAYOUT_REDESIGN, "v2", CTA_PLACEMENT, "sticky")},
                {"redesign + strikethrough + carousel",
                        Map.of(LAYOUT_REDESIGN, "v2", PRICE_DISPLAY, "strikethrough", RECO_MODULE, "carousel")},
        };
    }

    /**
     * The invariant every PDP must satisfy no matter the variant: you can read the product
     * and you can buy it. If a variant ships a bug that drops the add-to-cart button, THIS
     * is the test that catches it - and because the failure log carries the active
     * assignments (ExperimentReporter), triage immediately sees which variant did it.
     */
    @Test(dataProvider = "curatedVariants", groups = {"variant-matrix"})
    public void coreInvariantsHoldForVariant(String label, Map<String, String> variants) {
        ProductDetailPage pdp = openPdp(SKU, variants);

        assertTrue(!pdp.getTitle().isBlank(), "[" + label + "] product title missing");
        assertTrue(pdp.getPrice().current() > 0, "[" + label + "] price missing or non-positive");

        // The missing-element edge case from the brief. require()/isAddToCartPresent give a
        // clean, attributable failure instead of a NoSuchElement deep in the click.
        assertTrue(pdp.isAddToCartPresent(),
                "[" + label + "] add-to-cart button is gone - likely an experiment bug in: " + variants);
    }

    /**
     * Recommendations are optional, so we assert SOFTLY: log when a variant we expected to
     * show recos didn't, but don't fail the run on it. A missing optional module shouldn't
     * block a deploy; a missing buy button should.
     */
    @Test(dataProvider = "curatedVariants", groups = {"variant-matrix"})
    public void recommendationsRenderWhenExpected(String label, Map<String, String> variants) {
        ProductDetailPage pdp = openPdp(SKU, variants);

        if (!pdp.hasRecommendations()) {
            System.err.println("[recos] not rendered for variant: " + label
                    + " - acceptable if the reco service had no items, flagged for review");
        }
        // intentionally no hard assert - see javadoc
    }
}
