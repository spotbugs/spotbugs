package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue4020Test extends AbstractIntegrationTest {

    @Test
    void testObjectsNonNullNullCheckDischargesObligation() {
        performAnalysis("ghIssues/Issue4020.class");

        assertNoBugInMethod("OBL_UNSATISFIED_OBLIGATION", "ghIssues.Issue4020", "closeWithNullCheck");
        assertNoBugInMethod("OBL_UNSATISFIED_OBLIGATION", "ghIssues.Issue4020", "closeWithObjectsNonNull");
    }
}
