package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue4019Test extends AbstractIntegrationTest {

    @Test
    void testStatementSupertypeReportsResourceLeak() {
        performAnalysis("ghIssues/Issue4019.class");

        assertBugInMethod("OBL_UNSATISFIED_OBLIGATION", "ghIssues.Issue4019", "fetchUserBefore");
        assertBugInMethod("ODR_OPEN_DATABASE_RESOURCE", "ghIssues.Issue4019", "fetchUserBefore");
        assertBugInMethod("OBL_UNSATISFIED_OBLIGATION", "ghIssues.Issue4019", "fetchUserAfter");
        assertBugInMethod("ODR_OPEN_DATABASE_RESOURCE", "ghIssues.Issue4019", "fetchUserAfter");
    }
}
