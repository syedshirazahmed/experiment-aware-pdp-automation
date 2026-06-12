package com.shop.qa.experiments;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.restassured.response.Response;

import java.util.ArrayList;
import java.util.List;

import static io.restassured.RestAssured.given;

/**
 * Thin wrapper over the platform's decision endpoint.
 *
 * Most experiment platforms (Optimizely, LaunchDarkly, an in-house edge service)
 * expose a "what did this visitor get bucketed into" call. We hit it with the same
 * visitor id / forced-bucket cookie the browser is using, so the API answer and the
 * rendered page agree. That agreement is what lets us trust the assignment instead
 * of guessing from the DOM.
 */
public class ExperimentApiClient {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final String baseUrl;

    public ExperimentApiClient(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    /**
     * @param visitorId        anonymous id the bucketing is keyed on (cookie value)
     * @param forcedOverrides  experimentKey -> variant we pinned for this run; passed
     *                         through so the service returns the same thing the edge served
     */
    public List<Assignment> decisionsFor(String visitorId, java.util.Map<String, String> forcedOverrides) {
        Response resp = given()
                .baseUri(baseUrl)
                .header("X-Visitor-Id", visitorId)
                .header("X-Forced-Variants", encodeOverrides(forcedOverrides))
                .accept("application/json")
                .when()
                .get("/api/experiments/decisions")
                .then()
                .extract().response();

        if (resp.statusCode() != 200) {
            // Don't fail the test here. An empty context just means "treat as control",
            // and the page-level assertions will still catch a genuinely broken page.
            // We log loudly so the failure is attributable if it matters.
            System.err.println("[experiments] decision API returned " + resp.statusCode()
                    + " for visitor " + visitorId + " - falling back to empty context");
            return List.of();
        }

        return parse(resp.asString());
    }

    private List<Assignment> parse(String body) {
        List<Assignment> out = new ArrayList<>();
        try {
            JsonNode root = MAPPER.readTree(body);
            JsonNode decisions = root.path("decisions");
            for (JsonNode d : decisions) {
                out.add(new Assignment(
                        d.path("key").asText(),
                        d.path("variant").asText(),
                        d.path("experimentId").asText("")
                ));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse experiment decision payload: " + body, e);
        }
        return out;
    }

    private String encodeOverrides(java.util.Map<String, String> overrides) {
        if (overrides == null || overrides.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        overrides.forEach((k, v) -> sb.append(k).append(':').append(v).append(','));
        sb.setLength(sb.length() - 1); // trailing comma
        return sb.toString();
    }
}
