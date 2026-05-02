package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3929Test extends AbstractIntegrationTest {

    @Test
    void testNoFalsePositiveSaLocalSelfAssignmentInTrySwitchIinc() {
        performAnalysis("ghIssues/Issue3929.class");

        // Exactly 1 SA_LOCAL_SELF_ASSIGNMENT bug expected (the true positive only)
        assertBugTypeCount("SA_LOCAL_SELF_ASSIGNMENT", 1);

        // False positive (now fixed): ++a inside switch inside try-catch should NOT report SA
        assertNoBugInMethod("SA_LOCAL_SELF_ASSIGNMENT", "ghIssues.Issue3929", "reproduceFalsePositive");

        // Variant with multiple cases should also NOT report SA
        assertNoBugInMethod("SA_LOCAL_SELF_ASSIGNMENT", "ghIssues.Issue3929", "incrementBeforeCatch");

        // Variant with decrement should also NOT report SA
        assertNoBugInMethod("SA_LOCAL_SELF_ASSIGNMENT", "ghIssues.Issue3929", "decrementBeforeCatch");

        // True positive: real self-assignment must still be detected
        assertBugInMethod("SA_LOCAL_SELF_ASSIGNMENT", "ghIssues.Issue3929", "realSelfAssignment");
    }
}
