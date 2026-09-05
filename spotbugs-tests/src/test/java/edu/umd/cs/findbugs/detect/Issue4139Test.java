package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue4139Test extends AbstractIntegrationTest {

    private static final String BUG = "UR_UNINIT_READ";
    private static final String CLASS = "ghIssues.Issue4139";

    @Test
    void testCompoundAndExplicitAssignmentReportEqually() {
        performAnalysis("ghIssues/Issue4139.class");

        // Each form below reads an uninitialized field in a constructor and must be
        // reported: the '|=' and explicit 'x = x | ...' pair from the original issue,
        // plus '+=' and '++' which share the same ALOAD_0; DUP; GETFIELD pattern and
        // lock the fix for the whole compound-assignment family.
        assertBugTypeCount(BUG, 4);
        assertBugInMethodCount(BUG, CLASS, "<init>", 4);
    }
}
