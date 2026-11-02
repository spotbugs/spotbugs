package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue4407Test extends AbstractIntegrationTest {

    @Test
    void reportsWaitWhileTwoDistinctMonitorsAreHeld() {
        performAnalysis("ghIssues/Issue4407.class");

        assertBugTypeCount("IL_INFINITE_LOOP", 0);
    }
}
