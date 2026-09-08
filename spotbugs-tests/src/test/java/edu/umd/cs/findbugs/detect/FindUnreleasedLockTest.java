package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class FindUnreleasedLockTest extends AbstractIntegrationTest {

    @Test
    void doesNotFindSSDBugIfLockIsClosedInAProperWay() {
        performAnalysis("unreleasedLock/ProperlyClosedLock.class");
        assertNoBugType("UL_UNRELEASED_LOCK");
        assertNoBugType("UL_UNRELEASED_LOCK_EXCEPTION_PATH");
        assertNoBugType("CWO_CLOSED_WITHOUT_OPENED");
    }

    @Test
    void findPossibleUnlockCallWithoutLockCall() {
        performAnalysis("unreleasedLock/ThrowExceptionAndUnlockBeforeLock.class");
        assertNoBugType("UL_UNRELEASED_LOCK");
        assertNoBugType("UL_UNRELEASED_LOCK_EXCEPTION_PATH");
        assertBugTypeCount("CWO_CLOSED_WITHOUT_OPENED", 1);
        assertBugInMethod("CWO_CLOSED_WITHOUT_OPENED", "ThrowExceptionAndUnlockBeforeLock", "doSomething");
    }

    @Test
    void findUnresolvedLockOnExceptionalCondition() {
        performAnalysis("unreleasedLock/UnreleasedLock.class");
        assertNoBugType("UL_UNRELEASED_LOCK");
        assertNoBugType("CWO_CLOSED_WITHOUT_OPENED");
        assertBugTypeCount("UL_UNRELEASED_LOCK_EXCEPTION_PATH", 1);
        assertBugInMethod("UL_UNRELEASED_LOCK_EXCEPTION_PATH", "UnreleasedLock", "doSomething");
    }
}
