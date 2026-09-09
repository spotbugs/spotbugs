package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3966Test extends AbstractIntegrationTest {

    @Test
    void testDmStringToStringFalseNegativeInChainedExpressions() {
        performAnalysis("ghIssues/Issue3966.class");

        // All 3 methods contain redundant toString() calls on String instances and should be detected
        assertBugInMethod("DM_STRING_TOSTRING", "ghIssues.Issue3966", "singleToStringTP");
        assertBugInMethod("DM_STRING_TOSTRING", "ghIssues.Issue3966", "toStringMiddleTP");
        // Before fix: only s.toUpperCase().toString() was reported; s.toString().toLowerCase() was silently dropped
        // After fix: both occurrences are captured in the bug report
        assertBugInMethod("DM_STRING_TOSTRING", "ghIssues.Issue3966", "chainedToStringTP");
    }
}
