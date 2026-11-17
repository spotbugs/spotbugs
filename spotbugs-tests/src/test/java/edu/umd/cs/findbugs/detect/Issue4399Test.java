package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue4399Test extends AbstractIntegrationTest {

    @Test
    void testLockAcquiredForTheCallerIsNotReported() {
        performAnalysis("ghIssues/Issue4399.class");
        assertNoBugType("UL_UNRELEASED_LOCK");
        assertNoBugType("UL_UNRELEASED_LOCK_EXCEPTION_PATH");
        assertNoBugType("CWO_CLOSED_WITHOUT_OPENED");
    }
}
