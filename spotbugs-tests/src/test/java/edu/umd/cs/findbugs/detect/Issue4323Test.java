package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue4323Test extends AbstractIntegrationTest {

    @Test
    void reportsWaitWhileTwoDistinctMonitorsAreHeld() {
        performAnalysis("ghIssues/Issue4323.class");

        assertBugTypeCount("TLW_TWO_LOCK_WAIT", 1);
        assertBugInMethodAtLine("TLW_TWO_LOCK_WAIT", "Issue4323", "waitWithTwoLocks", 10);
    }
}
