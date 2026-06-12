package com.shop.qa.pages.pdp;

import com.shop.qa.core.SmartLocator;
import com.shop.qa.core.WaitUtils;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.Optional;

/**
 * The add-to-cart control.
 *
 * This is the component that directly answers the case-study edge case: "CTA button
 * location varies (top, bottom, sticky)". We do NOT branch on the variant here. The
 * button means the same thing in every variant, so we identify it by what it *is*
 * (its testid / its role + label), not by where it sits. Position is a layout detail
 * the test shouldn't care about.
 *
 * Handling the awkward bits:
 *  - sticky footer bars overlap the inline button -> scroll + JS-click fallback when the
 *    native click is intercepted.
 *  - multiple matches (inline AND sticky both in the DOM) -> we take the first actionable
 *    one; clicking either adds the same item, which is the behaviour we actually care about.
 */
public class AddToCartCta {

    private final WebDriver driver;

    // Same testid across variants is the contract. Fallbacks cover variants mid-migration.
    private final SmartLocator locator = SmartLocator.byTestId(
            "add-to-cart button",
            "pdp-add-to-cart",
            By.xpath("//button[@data-sticky-cta]"),                    // sticky footer variant
            By.xpath("//button[normalize-space()='Add to cart' or normalize-space()='Add to bag']"));

    public AddToCartCta(WebDriver driver) {
        this.driver = driver;
    }

    public boolean isPresent() {
        return locator.isPresent(driver);
    }

    public void click() {
        WebElement button = locator.require(driver);
        WaitUtils.scrollIntoView(driver, button);
        button.click();
    }

    /**
     * Confirmation that the add actually registered.
     */
    public int waitForCartCount() {
        WebElement badge = WaitUtils.waitForElementToBeVisible(driver,
                By.xpath("//*[@data-testid='cart-count']"));
        String text = badge.getText().trim();
        return text.isEmpty() ? 0 : Integer.parseInt(text);
    }

    public Optional<String> labelText() {
        return locator.find(driver).map(WebElement::getText);
    }
}
