package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3904Test extends AbstractIntegrationTest {

    @Test
    void testIlInfiniteLoopFalsePositiveForAlwaysFalseCondition() {
        performAnalysis("ghIssues/Issue3904.class");

        // innerLoopAlwaysFalse: inner do-while condition (j < 1 with j == 1) is always false.
        // The inner loop exits immediately — NOT infinite. Only the outer loop should be reported.
        assertBugInMethodCount("IL_INFINITE_LOOP", "ghIssues.Issue3904", "innerLoopAlwaysFalse", 1);
        assertBugInMethodAtLine("IL_INFINITE_LOOP", "ghIssues.Issue3904", "innerLoopAlwaysFalse", 18);

        // innerLoopAlwaysTrue: inner do-while condition (j <= 1 with j == 1) is always true.
        // The inner loop is truly infinite — should be reported.
        assertBugInMethod("IL_INFINITE_LOOP", "ghIssues.Issue3904", "innerLoopAlwaysTrue");

        // singleLoopAlwaysFalse: do-while condition (i < 1 with i == 1) is always false.
        // The loop exits after its first iteration — NOT infinite, should NOT be reported.
        assertNoBugInMethod("IL_INFINITE_LOOP", "ghIssues.Issue3904", "singleLoopAlwaysFalse");

        // singleLoopAlwaysTrue: do-while condition (i < 5 with i == 1) is always true.
        // The loop is truly infinite — should be reported.
        assertBugInMethod("IL_INFINITE_LOOP", "ghIssues.Issue3904", "singleLoopAlwaysTrue");
    }
}
