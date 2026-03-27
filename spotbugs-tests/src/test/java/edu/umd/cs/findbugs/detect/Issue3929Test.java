package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3929Test extends AbstractIntegrationTest {

    @Test
    void testNoFalsePositiveSaLocalSelfAssignmentInTrySwitchIinc() {
        performAnalysis("ghIssues/Issue3929.class");

        // False positive (now fixed): ++a inside switch inside try-catch should NOT report SA
        assertNoBugInMethod("SA_LOCAL_SELF_ASSIGNMENT", "ghIssues.Issue3929", "reproduceFalsePositive");

        // Variant with multiple cases should also NOT report SA
        assertNoBugInMethod("SA_LOCAL_SELF_ASSIGNMENT", "ghIssues.Issue3929", "incrementBeforeCatch");

        // True positive: real self-assignment must still be detected
        assertBugInMethod("SA_LOCAL_SELF_ASSIGNMENT", "ghIssues.Issue3929", "realSelfAssignment");
    }
}
