package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue3786Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue3786.class");

        assertBugInMethodCount("NN_NAKED_NOTIFY", "ghIssues.Issue3786", "execute", 0);
        assertBugInMethodCount("NN_NAKED_NOTIFY", "ghIssues.Issue3786", "execute1", 0);
        assertBugInMethodCount("NN_NAKED_NOTIFY", "ghIssues.Issue3786", "executeOk", 0);
        assertBugInMethodCount("NN_NAKED_NOTIFY", "ghIssues.Issue3786", "execute1Ok", 0);
        assertBugInMethodCount("NN_NAKED_NOTIFY", "ghIssues.Issue3786", "nakedNotify", 1);
        assertBugInMethodCount("NN_NAKED_NOTIFY", "ghIssues.Issue3786", "nonVolatileBeforeNotify", 1);
    }
}
