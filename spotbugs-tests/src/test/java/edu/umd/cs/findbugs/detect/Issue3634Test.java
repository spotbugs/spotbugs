package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue3634Test extends AbstractIntegrationTest {

    @Test
    void testRecordsSuppressions() {
        performAnalysis("ghIssues/Issue3634.class");

        assertBugInMethodCount("NN_NAKED_NOTIFY", "ghIssues.Issue3634", "fieldLock", 1);
        assertBugInMethodCount("NN_NAKED_NOTIFY", "ghIssues.Issue3634", "fieldOfStaticFieldLock", 1);
        assertBugInMethodCount("NN_NAKED_NOTIFY", "ghIssues.Issue3634", "fieldOfFieldLock", 1);
        assertBugInMethodCount("NN_NAKED_NOTIFY", "ghIssues.Issue3634", "chainedFieldsLock", 1);
        assertBugInMethodCount("NN_NAKED_NOTIFY", "ghIssues.Issue3634", "staticFieldLock", 1);

        assertBugInMethodCount("NN_NAKED_NOTIFY", "ghIssues.Issue3634", "fieldLockWithMutation", 0);
        assertBugInMethodCount("NN_NAKED_NOTIFY", "ghIssues.Issue3634", "chainedFieldsLockWithMutation", 0);
    }
}
