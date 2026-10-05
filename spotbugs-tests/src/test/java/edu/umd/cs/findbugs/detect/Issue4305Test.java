package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue4305Test extends AbstractIntegrationTest {

    private static final String BUG_TYPE = "AA_ASSERTION_OF_ARGUMENTS";

    @Test
    void instanceofCheckOfPublicParameterIsReported() {
        performAnalysis("ghIssues/Issue4305.class", "ghIssues/Issue4305Outer.class",
                "ghIssues/Issue4305Outer$Subject.class");

        assertBugInMethod(BUG_TYPE, "Issue4305", "convert");
        assertBugInMethod(BUG_TYPE, "Issue4305", "convertStatic");
        assertNoBugInMethod(BUG_TYPE, "Issue4305", "assertReceiver");
        assertNoBugInMethod(BUG_TYPE, "Issue4305", "convertOther");
        assertNoBugInMethod(BUG_TYPE, "Issue4305", "privateConvert");
        assertBugInMethod(BUG_TYPE, "Issue4305Outer$Subject", "convert");
        assertBugTypeCount(BUG_TYPE, 3);
    }
}
