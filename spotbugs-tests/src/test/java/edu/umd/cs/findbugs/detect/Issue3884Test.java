package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3884Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue3884.class");

        assertBugInMethodCount("NN_NAKED_NOTIFY", "ghIssues.Issue3884", "testWithFieldRead", 1);
        assertBugInMethodCount("NN_NAKED_NOTIFY", "ghIssues.Issue3884", "testWithPrintln", 1);
        assertBugInMethodCount("NN_NAKED_NOTIFY", "ghIssues.Issue3884", "testWithMutation", 0);
        assertBugInMethodCount("NN_NAKED_NOTIFY", "ghIssues.Issue3884", "testNakedNotify", 1);
    }
}
