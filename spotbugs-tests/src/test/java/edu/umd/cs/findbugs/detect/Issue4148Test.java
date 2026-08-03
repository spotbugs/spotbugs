package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue4148Test extends AbstractIntegrationTest {

    private static final String BUG_TYPE = "OBL_UNSATISFIED_OBLIGATION";
    private static final String CLASS_NAME = "ghIssues.Issue4148";

    @Test
    void detectsUnclosedCallableStatements() {
        performAnalysis("ghIssues/Issue4148.class");

        assertBugTypeCount(BUG_TYPE, 3);
        assertBugInMethod(BUG_TYPE, CLASS_NAME, "prepareCallNotClosed");
        assertBugInMethod(BUG_TYPE, CLASS_NAME, "prepareCallWithResultSetOptionsNotClosed");
        assertBugInMethod(BUG_TYPE, CLASS_NAME, "prepareCallWithHoldabilityNotClosed");
        assertNoBugInMethod(BUG_TYPE, CLASS_NAME, "prepareCallClosed");
    }
}
