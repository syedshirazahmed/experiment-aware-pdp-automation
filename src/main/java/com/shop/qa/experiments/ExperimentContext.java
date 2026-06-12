package com.shop.qa.experiments;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The set of experiment assignments that are live for one browser session.
 *
 * Page objects and tests query this instead of sniffing the DOM to figure out
 * "which world am I in". Built once per test by {@link ExperimentResolver} and
 * carried on the test thread.
 *
 * Deliberately read-only after construction
 */
public final class ExperimentContext {

    private final Map<String, Assignment> byKey;

    private ExperimentContext(Map<String, Assignment> byKey) {
        this.byKey = Collections.unmodifiableMap(byKey);
    }

    public static ExperimentContext of(Collection<Assignment> assignments) {
        Map<String, Assignment> map = new LinkedHashMap<>();
        for (Assignment a : assignments) {
            map.put(a.key(), a);
        }
        return new ExperimentContext(map);
    }

    public static ExperimentContext empty() {
        return new ExperimentContext(new LinkedHashMap<>());
    }

    /** Variant the session is in, or null if the experiment isn't running for this user. */
    public String variantOf(String experimentKey) {
        Assignment a = byKey.get(experimentKey);
        return a == null ? null : a.variant();
    }

    public boolean isVariant(String experimentKey, String variant) {
        return variant.equals(variantOf(experimentKey));
    }

    /**
     * True when the experiment is running and the user is NOT in control.
     * Most "does the new thing show up" checks key off this.
     */
    public boolean isInTreatment(String experimentKey) {
        String v = variantOf(experimentKey);
        return v != null && !"control".equalsIgnoreCase(v);
    }

    public Collection<Assignment> all() {
        return byKey.values();
    }

    public boolean isEmpty() {
        return byKey.isEmpty();
    }

    @Override
    public String toString() {
        return byKey.values().toString();
    }
}
