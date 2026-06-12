package com.shop.qa.pages.pdp;

import com.shop.qa.core.SmartLocator;
import com.shop.qa.experiments.ExperimentContext;
import com.shop.qa.experiments.PdpExperiments;
import com.shop.qa.pages.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

import java.util.Optional;

/**
 * The Page Object for the PDP.
 *
 * It's a HYBRID page object on purpose:
 *
 *   - {@code @FindBy} fields below for the simple, stable element that's present and in the
 *     same place on every variant (the current price). PageFactory wires and waits on it -
 *     this is the canonical POM and is most widely used.
 *
 *   - {@link SmartLocator} for everything the experiments actually move: the title (which a
 *     redesign relocates), the add-to-cart CTA (top/bottom/sticky), and the OPTIONAL bits
 *     (compare-at price, recommendations) where "not found" is a valid answer we want back
 *     fast rather than after a full PageFactory timeout.
 *
 * Rule of thumb encoded here: if a locator is stable across variants, declare it with
 * {@code @FindBy}; the moment it varies or might be absent, reach for SmartLocator.
 */
public class ProductDetailPage extends BasePage {

    // PageFactory (classic POM)
    @FindBy(xpath = "//*[@data-testid='pdp-price-current']")
    private WebElement currentPrice;

    // SmartLocator (variant sensitive)
    private final AddToCartCta cta;

    public ProductDetailPage(WebDriver driver, ExperimentContext experiments) {
        super(driver, experiments); // BasePage runs PageFactory.initElements for @FindBy fields
        this.cta = new AddToCartCta(driver);
    }

    public String getTitle() {
        return titleLocator().require(driver).getText().trim();
    }

    /**
     * Reads price independent of the display experiment. The current price is a stable
     * testid (PageFactory field). The compare-at "was" price only exists in the
     * strikethrough variant, so it's an optional SmartLocator lookup - absence is normal,
     * not a failure.
     */
    public Price getPrice() {
        double current = parseMoney(currentPrice.getText());

        Optional<WebElement> original =
                SmartLocator.byTestId("original price", "pdp-price-original").find(driver);

        return new Price(current, original.map(e -> parseMoney(e.getText())).orElse(null));
    }

    public void addToCart() {
        cta.click();
    }

    public int cartCountAfterAdd() {
        return cta.waitForCartCount();
    }

    public boolean isAddToCartPresent() {
        return cta.isPresent();
    }

    /**
     * Recommendations are an OPTIONAL module - some variants drop it entirely, and the
     * reco service occasionally returns nothing for a sparse-catalogue product. So this
     * returns a boolean and tests treat absence as a soft skip, never a hard failure.
     * Grid vs carousel share the testid, so we don't care which rendered.
     */
    public boolean hasRecommendations() {
        return SmartLocator.byTestId("recommendations module", "pdp-reco-module").isPresent(driver);
    }

    // --- internals -------------------------------------------------------------------

    private SmartLocator titleLocator() {
        // The redesign moves the title into a new header component with its own testid.
        // This is the one structural fork; everything else rides the shared facade above.
        if (experiments.isVariant(PdpExperiments.LAYOUT_REDESIGN, "v2")) {
            return SmartLocator.byTestId("product title (v2)", "pdp-v2-title");
        }
        return SmartLocator.byTestId("product title", "pdp-title",
                By.xpath("//h1[contains(concat(' ', normalize-space(@class), ' '), ' product-name ')]"));
    }

    /** "$1,299.00" / "₹1,299" / "1299" -> 1299.0. Currency- and separator-tolerant. */
    private double parseMoney(String raw) {
        String digits = raw.replaceAll("[^0-9.]", "");
        if (digits.isEmpty()) {
            throw new IllegalStateException("No numeric price found in: '" + raw + "'");
        }
        return Double.parseDouble(digits);
    }
}
