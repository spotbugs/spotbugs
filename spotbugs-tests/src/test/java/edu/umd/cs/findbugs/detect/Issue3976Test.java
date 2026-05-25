package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3976Test extends AbstractIntegrationTest {

    @Test
    void testNonShortCircuitWithConstantLeftOperand() {
        performAnalysis("ghIssues/Issue3976.class");

        assertBugInMethod("NS_NON_SHORT_CIRCUIT", "ghIssues.Issue3976", "testTruePositive");
        assertBugInMethod("NS_NON_SHORT_CIRCUIT", "ghIssues.Issue3976", "testFalseNegative");
        assertBugInMethod("NS_NON_SHORT_CIRCUIT", "ghIssues.Issue3976", "testConstantFalseAnd");
        assertNoBugInMethod("NS_NON_SHORT_CIRCUIT", "ghIssues.Issue3976", "bothConstants");
        assertNoBugInMethod("NS_NON_SHORT_CIRCUIT", "ghIssues.Issue3976", "bothConstants");
    }
}
