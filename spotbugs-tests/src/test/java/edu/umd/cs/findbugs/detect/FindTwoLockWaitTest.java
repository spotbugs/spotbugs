package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class FindTwoLockWaitTest extends AbstractIntegrationTest {

    @Test
    void reportsWaitWhileHoldingDistinctMonitors() {
        performAnalysis("TwoLockWaitDistinctMonitors.class");

        assertBugTypeCount("TLW_TWO_LOCK_WAIT", 1);
        assertBugInMethod("TLW_TWO_LOCK_WAIT", "TwoLockWaitDistinctMonitors", "waitWithTwoLocks");
    }

    @Test
    void doesNotReportWaitWhileReenteringTheSameMonitor() {
        performAnalysis("TwoLocksWhileWaitingFalsePositive.class");

        assertNoBugType("TLW_TWO_LOCK_WAIT");
    }
}
