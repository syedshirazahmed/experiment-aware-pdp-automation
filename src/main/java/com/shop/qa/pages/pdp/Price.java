package com.shop.qa.pages.pdp;

/**
 * Normalised price reading, so tests assert on numbers regardless of how the price
 * experiment chooses to render.
 */
public final class Price {

    private final double current;
    private final Double original; // null when there's no compare-at price

    public Price(double current, Double original) {
        this.current = current;
        this.original = original;
    }

    public double current() {
        return current;
    }

    public boolean hasDiscount() {
        return original != null && original > current;
    }

    public double savings() {
        return hasDiscount() ? original - current : 0d;
    }

    @Override
    public String toString() {
        return hasDiscount() ? (current + " (was " + original + ")") : String.valueOf(current);
    }
}
