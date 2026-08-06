package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue4139Test extends AbstractIntegrationTest {

    private static final String BUG = "UR_UNINIT_READ";
    private static final String CLASS = "ghIssues.Issue4139";

    @Test
    void testCompoundAndExplicitAssignmentReportEqually() {
        performAnalysis("ghIssues/Issue4139.class");

        // Both the compound form '|=' and the equivalent explicit form 'x = x | ...'
        // read the uninitialized field in the constructor and must be reported.
        assertBugTypeCount(BUG, 2);
        assertBugInMethodCount(BUG, CLASS, "<init>", 2);
    }
}
