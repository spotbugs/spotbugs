package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue4037Test extends AbstractIntegrationTest {

    @Test
    void testBooleanReturnNullViaLocalVariable() {
        performAnalysis("ghIssues/Issue4037.class");

        assertBugInMethod("NP_BOOLEAN_RETURN_NULL", "ghIssues.Issue4037", "isValidDirect");
        assertBugInMethod("NP_BOOLEAN_RETURN_NULL", "ghIssues.Issue4037", "isValidIndirect");
        assertBugInMethod("NP_BOOLEAN_RETURN_NULL", "ghIssues.Issue4037", "checkStatus");
        assertNoBugInMethod("NP_BOOLEAN_RETURN_NULL", "ghIssues.Issue4037", "neverNull");
    }
}
