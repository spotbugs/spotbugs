package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3170Test extends AbstractIntegrationTest {

    @Test
    void testObligationLeakInHelperClass() {
        performAnalysis("ghIssues/Issue3170MyConnection.class", "ghIssues/Issue3170Tool.class");
        assertBugInMethod("OBL_UNSATISFIED_OBLIGATION", "ghIssues.Issue3170MyConnection", "leak");
        assertBugInMethod("OBL_UNSATISFIED_OBLIGATION", "ghIssues.Issue3170Tool", "leak");
    }
}
