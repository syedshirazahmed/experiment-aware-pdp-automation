package com.shop.qa.experiments;

import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebDriver;

import java.util.Map;
import java.util.UUID;

/**
 * Owns the "make experiments deterministic" problem.
 *
 *  1. PIN the buckets we care about via a forced-variant cookie that our edge respects
 *     (same mechanism QA uses to preview variants manually). Now the render is stable.
 *  2. READ BACK what the visitor actually got from the decision API and build an
 *     {@link ExperimentContext}. Reading back matters - if the pin silently failed,
 *     the context won't match what we asked for and the test fails for the right reason.
 *
 * Cookie/visitor approach is intentional over query params: query params don't survive
 * client-side navigation, cookies do, so the variant stays pinned across PDP -> cart -> checkout.
 */
public class ExperimentResolver {

    public static final String VISITOR_COOKIE = "vid";
    public static final String OVERRIDE_COOKIE = "ff_overrides";

    private final ExperimentApiClient apiClient;

    public ExperimentResolver(ExperimentApiClient apiClient) {
        this.apiClient = apiClient;
    }

    /**
     * Pins the requested experiment->variant choices for this session. Must be called
     * while the driver is already on the target domain (cookies are domain-scoped),
     * so callers usually hit the home page first, pin, then navigate to the PDP.
     *
     * @return the visitor id we generated, so the API client can ask about the same visitor
     */
    public String pin(WebDriver driver, Map<String, String> overrides) {
        String visitorId = UUID.randomUUID().toString();
        driver.manage().addCookie(new Cookie(VISITOR_COOKIE, visitorId));
        driver.manage().addCookie(new Cookie(OVERRIDE_COOKIE, encode(overrides)));
        return visitorId;
    }

    /**
     * Builds the context for the session by asking the decision API what the pinned
     * visitor resolved to. Pass the overrides through so the API and the edge agree.
     */
    public ExperimentContext resolve(String visitorId, Map<String, String> overrides) {
        return ExperimentContext.of(apiClient.decisionsFor(visitorId, overrides));
    }

    private String encode(Map<String, String> overrides) {
        if (overrides == null || overrides.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        overrides.forEach((k, v) -> sb.append(k).append(':').append(v).append(','));
        sb.setLength(sb.length() - 1);
        return sb.toString();
    }
}
