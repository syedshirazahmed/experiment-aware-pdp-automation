package com.shop.qa.experiments;

/**
 * Catalogue of the experiments currently touching the PDP.
 *
 * Keeping the keys in one place (instead of scattering string literals through tests)
 * means when product renames "pdp_cta_placement" we change it once. It also doubles as
 * documentation of what's actually live.
 */
public final class PdpExperiments {

    private PdpExperiments() {
    }

    // Moves the add-to-cart button: control = inline below price, "sticky" = pinned footer bar.
    public static final String CTA_PLACEMENT = "pdp_cta_placement";

    // Pricing display test: control = single price, "strikethrough" = was/now with savings badge.
    public static final String PRICE_DISPLAY = "pdp_price_display";

    // Recommendations module: control = "similar items" grid, "carousel" = horizontal scroller.
    public static final String RECO_MODULE = "pdp_reco_module";

    // Whole-page redesign. This one swaps the DOM structure, not just styling - the case
    // where a shared facade isn't enough and we branch the page object (see ProductDetailPage).
    public static final String LAYOUT_REDESIGN = "pdp_layout_v2";
}
