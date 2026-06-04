package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue3976Test extends AbstractIntegrationTest {

    @Test
    void testNonShortCircuitWithConstantTrueOperand() {
        performAnalysis("ghIssues/Issue3976.class");

        assertBugInMethodCount("NS_NON_SHORT_CIRCUIT", "Issue3976", "testTruePositive", 1);
        assertBugInMethodCount("NS_NON_SHORT_CIRCUIT", "Issue3976", "testFalseNegative", 1);
    }
}
