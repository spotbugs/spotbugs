package edu.umd.cs.findbugs.ba;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue2995Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue2995.class");

        assertNoBugType("SF_DEAD_STORE_DUE_TO_SWITCH_FALLTHROUGH");
    }
}
