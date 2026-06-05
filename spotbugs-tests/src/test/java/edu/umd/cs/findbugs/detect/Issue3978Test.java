package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3978Test extends AbstractIntegrationTest {

    @Test
    void testInfiniteLoopFalsePositiveWithConstantBreak() {
        performAnalysis("ghIssues/Issue3978.class");

        assertNoBugInMethod("IL_INFINITE_LOOP", "ghIssues.Issue3978", "constantIfElseBreak");
        assertBugInMethod("IL_INFINITE_LOOP", "ghIssues.Issue3978", "constantIfElseContinue");
    }
}
