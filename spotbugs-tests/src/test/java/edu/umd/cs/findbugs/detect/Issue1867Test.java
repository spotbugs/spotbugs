package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue1867Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue1867.class");
        assertNoBugType("MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR");
    }
}
