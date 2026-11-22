package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Regression test for GitHub issue #4465.
 */
class Issue4465Test extends AbstractIntegrationTest {

    @Test
    void testBigDecimalThroughSingletonList() {
        performAnalysis("ghIssues/Issue4465.class");

        assertBugInMethod(
                "DMI_BIGDECIMAL_CONSTRUCTED_FROM_DOUBLE",
                "Issue4465", "direct");

        assertBugInMethod(
                "DMI_BIGDECIMAL_CONSTRUCTED_FROM_DOUBLE",
                "Issue4465", "throughSingletonList");
    }
}
