package com.shop.qa.pages;

import com.shop.qa.experiments.ExperimentContext;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.pagefactory.AjaxElementLocatorFactory;

/**
 * Base for all page objects. Two responsibilities:
 *
 *  1. Page Object Model wiring. The constructor runs PageFactory.initElements, so any
 *     {@code @FindBy} field a subclass declares is populated automatically - the classic
 *     POM style. We use {@link AjaxElementLocatorFactory} rather than the plain factory so
 *     those fields come with a built-in visibility wait; that's what lets simple, stable
 *     elements skip explicit WaitUtils calls.
 *
 *  2. Experiment awareness. Every page knows the variant context it's running under, so the
 *     *tests* don't have to. A test says addToCart(); the page object decides whether that's
 *     a sticky footer button or an inline one based on the variant it was handed.
 *
 * The framework is deliberately a HYBRID: {@code @FindBy} for the simple, stable, present-on-
 * every-variant elements (canonical POM), and {@link com.shop.qa.core.SmartLocator} for the
 * variant-sensitive ones that need ordered fallbacks or a structural branch. PageFactory
 * binds one locator per field up front and caches the reference, which is great for stable
 * elements but exactly wrong for elements that move or re-render between variants - hence the
 * split.
 */
public abstract class BasePage {

    private static final int DEFAULT_WAIT_SECONDS = 10;

    protected final WebDriver driver;
    protected final ExperimentContext experiments;

    protected BasePage(WebDriver driver, ExperimentContext experiments) {
        this.driver = driver;
        this.experiments = experiments;
        PageFactory.initElements(driver,this);
    }

    public ExperimentContext experiments() {
        return experiments;
    }
}
