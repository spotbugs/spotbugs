package edu.umd.cs.findbugs.nullness;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class NullableFutureTest extends AbstractIntegrationTest {
    @Test
    void testIssue() {
        performAnalysis("nullnessAnnotations/NullableFuture.class");
        assertNoBugType("NP_NONNULL_PARAM_VIOLATION");
    }

    @Test
    void testIssue1397() {
        performAnalysis("../java11/ghIssues/Issue1397.class");
        assertNoBugType("NP_NONNULL_PARAM_VIOLATION");
    }
}
