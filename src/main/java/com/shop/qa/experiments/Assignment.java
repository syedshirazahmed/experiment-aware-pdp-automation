package com.shop.qa.experiments;

import java.util.Objects;

/**
 * A single experiment -> variant assignment for the current session.
 *
 * key          stable experiment key, e.g. "pdp_cta_placement"
 * variant      the bucket the user landed in, e.g. "sticky"
 * experimentId the platform id (Optimizely/LaunchDarkly) - handy for log attribution
 */
public final class Assignment {

    private final String key;
    private final String variant;
    private final String experimentId;

    public Assignment(String key, String variant, String experimentId) {
        this.key = key;
        this.variant = variant;
        this.experimentId = experimentId;
    }

    public String key() {
        return key;
    }

    public String variant() {
        return variant;
    }

    public String experimentId() {
        return experimentId;
    }

    @Override
    public String toString() {
        // shows up in failure logs, keep it scannable
        return key + "=" + variant + " (" + experimentId + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Assignment)) return false;
        Assignment that = (Assignment) o;
        return key.equals(that.key) && variant.equals(that.variant);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, variant);
    }
}
