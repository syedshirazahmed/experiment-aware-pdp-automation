package com.shop.qa.support;

import com.shop.qa.experiments.Assignment;
import com.shop.qa.experiments.ExperimentContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

/**
 * Observability glue. The whole point of an experiment-aware framework is being able to
 * answer "which experiment broke this?" the moment a test goes red - without that, a
 * failure on a 20-variant page is a needle in a haystack.
 *
 * On every failure we attach the exact assignment set to the result. In a real setup these
 * tags flow into the test report (Allure label / JUnit XML property) and into the failure
 * event we ship to the dashboard, so flake triage can group failures by experiment and spot
 * "everything in pdp_layout_v2 is failing" at a glance. Here we log them; the hook points
 * are marked.
 */
public class ExperimentReporter implements ITestListener {

    @Override
    public void onTestFailure(ITestResult result) {
        ExperimentContext ctx = TestContextHolder.get();
        String testName = result.getMethod().getQualifiedName();

        StringBuilder sb = new StringBuilder();
        sb.append("\n==== FAILURE: ").append(testName).append(" ====\n");
        if (ctx.isEmpty()) {
            sb.append("  active experiments: <none / control>\n");
        } else {
            sb.append("  active experiments:\n");
            for (Assignment a : ctx.all()) {
                sb.append("    - ").append(a).append('\n');
                // result.setAttribute(...) / Allure.label("experiment", a.key()+":"+a.variant())
                // would surface this in the report and the observability pipeline.
            }
        }
        sb.append("  cause: ").append(rootMessage(result.getThrowable()));
        System.err.println(sb);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        // Pass data is just as useful for coverage: lets us answer "did we actually exercise
        // the sticky-CTA variant this run, or did bucketing never put us there?"
    }

    private String rootMessage(Throwable t) {
        if (t == null) return "(none)";
        Throwable cur = t;
        while (cur.getCause() != null && cur.getCause() != cur) {
            cur = cur.getCause();
        }
        return cur.getClass().getSimpleName() + ": " + cur.getMessage();
    }
}
