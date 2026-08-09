package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue3884Test extends AbstractIntegrationTest {

    @Test
    void testNakedNotifyAfterNonMutatingRead() {
        performAnalysis("ghIssues/Issue3884.class");

        assertBugInMethodCount("NN_NAKED_NOTIFY", "ghIssues.Issue3884", "notifyAfterFieldRead", 1);
        assertBugInMethodCount("NN_NAKED_NOTIFY", "ghIssues.Issue3884", "blockNotifyAfterFieldRead", 1);
        assertBugInMethodCount("NN_NAKED_NOTIFY", "ghIssues.Issue3884", "nakedNotify", 1);

        assertBugInMethodCount("NN_NAKED_NOTIFY", "ghIssues.Issue3884", "notifyAfterMutation", 0);
        assertBugInMethodCount("NN_NAKED_NOTIFY", "ghIssues.Issue3884", "notifyAfterFieldAssignment", 0);
    }
}
