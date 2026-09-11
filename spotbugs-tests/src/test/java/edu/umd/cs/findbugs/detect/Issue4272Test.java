package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue4272Test extends AbstractIntegrationTest {

    @Test
    void testKnownInstanceofValues() {
        performAnalysis("ghIssues/Issue4272.class");

        assertNoBugInMethod("NP_ALWAYS_NULL", "ghIssues.Issue4272", "knownInstanceof");
        assertNoBugInMethod("NP_ALWAYS_NULL", "ghIssues.Issue4272", "knownNegatedInstanceof");
        assertNoBugInMethod("NP_ALWAYS_NULL", "ghIssues.Issue4272", "unknownInstanceof");
    }
}
