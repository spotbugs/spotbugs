package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3786Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue3786.class");

        assertBugInMethodCount("NN_NAKED_NOTIFY", "ghIssues.Issue3786", "executeFalsePositive", 0);
        assertBugInMethodCount("NN_NAKED_NOTIFY", "ghIssues.Issue3786", "execute1FalsePositive", 0);
        assertBugInMethodCount("NN_NAKED_NOTIFY", "ghIssues.Issue3786", "executeOk", 0);
        assertBugInMethodCount("NN_NAKED_NOTIFY", "ghIssues.Issue3786", "execute1Ok", 0);
        assertBugInMethodCount("NN_NAKED_NOTIFY", "ghIssues.Issue3786", "executeTruePositive", 1);
    }
}
