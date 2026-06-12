package com.shop.qa.core;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * A locator that tries several strategies in priority order and uses the first that hits.
 *
 * This is how we survive layout experiments. The contract with the front-end team is that
 * every interactive element carries a stable data-testid that does NOT change between
 * variants - that's always strategy #1. The extra fallbacks (role, accessible text) are a
 * safety net for variants that ship before the testid lands, or legacy corners that never
 * got one. We log when we fall through to a weaker strategy so the gap gets noticed and
 * the testid gets added, rather than quietly rotting.
 *
 * Order matters: semantic/stable first, brittle/positional never. There is intentionally
 * no "nth-child" or absolute-xpath option here - if you find yourself wanting one, the fix
 * is a testid, not a cleverer selector.
 */
public final class SmartLocator {

    private final String description;
    private final List<By> strategies;

    private SmartLocator(String description, List<By> strategies) {
        this.description = description;
        this.strategies = strategies;
    }

    /** Preferred: build from a testid plus optional human-readable fallbacks. */
    public static SmartLocator byTestId(String description, String testId, By... fallbacks) {
        By primary = By.xpath("//*[@data-testid='" + testId + "']");
        List<By> all = new java.util.ArrayList<>();
        all.add(primary);
        all.addAll(Arrays.asList(fallbacks));
        return new SmartLocator(description, all);
    }

    /**
     * Finds the element, waiting on each strategy briefly before moving on. Returns
     * empty rather than throwing - callers decide whether "not found" is a soft skip
     * (optional module) or a hard failure (missing add-to-cart button = experiment bug).
     */
    public Optional<WebElement> find(WebDriver driver) {
        for (int i = 0; i < strategies.size(); i++) {
            By by = strategies.get(i);
            // short per-strategy wait; the first one should normally win
            try {
                WebElement el = WaitUtils.waitForElementToBeVisible(driver, by, Duration.ofSeconds(i == 0 ? 6 : 2));
                if (i > 0) {
                    System.err.println("[locator] '" + description + "' fell back to strategy #"
                            + (i + 1) + " (" + by + "). Add a data-testid to harden this.");
                }
                return Optional.of(el);
            } catch (Exception ignored) {
                // try next strategy
            }
        }
        return Optional.empty();
    }

    /** Same as find() but blows up with a readable message - use when the element is mandatory. */
    public WebElement require(WebDriver driver) {
        return find(driver).orElseThrow(() -> new NoSuchElementException(
                "Could not locate '" + description + "' using any known strategy: " + strategies));
    }

    public boolean isPresent(WebDriver driver) {
        return find(driver).isPresent();
    }

    public String description() {
        return description;
    }
}
