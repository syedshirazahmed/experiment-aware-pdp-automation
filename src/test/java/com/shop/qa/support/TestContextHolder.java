package com.shop.qa.support;

import com.shop.qa.experiments.ExperimentContext;

/**
 * Per-thread stash of the experiment context, so the TestNG listener can read which
 * variants were active when a test failed without us threading that data through every
 * signature. Set in BaseTest, read in ExperimentReporter.
 */
public final class TestContextHolder {

    private static final ThreadLocal<ExperimentContext> CONTEXT = new ThreadLocal<>();

    private TestContextHolder() {
    }

    public static void set(ExperimentContext ctx) {
        CONTEXT.set(ctx);
    }

    public static ExperimentContext get() {
        ExperimentContext ctx = CONTEXT.get();
        return ctx == null ? ExperimentContext.empty() : ctx;
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
