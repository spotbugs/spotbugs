package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

public class FindUnreleasedLockTest extends AbstractIntegrationTest {

    @Test
    public void doesNotFindSSDBug_ifLockIsClosedInAProperWay() {
        performAnalysis("unreleasedLock/ProperlyClosedLock.class");

        assertAnalyzedClassHasNoBugs();
    }

    @Test
    public void findPossibleUnlockCall_withoutLockCall() {
        //SystemProperties.setProperty("ful.debug", "true");
        performAnalysis("unreleasedLock/ThrowExceptionAndUnlockBeforeLock.class");

        assertAnalyzedClassHasBug("CWO_CLOSED_WITHOUT_OPENED", 1);
        assertExactPlaceOfBug("ThrowExceptionAndUnlockBeforeLock", "doSomething", "CWO_CLOSED_WITHOUT_OPENED");
    }

    @Test
    public void findUnresolvedLock_onExceptionalCondition() {
        performAnalysis("unreleasedLock/UnreleasedLock.class");

        assertAnalyzedClassHasBug("UL_UNRELEASED_LOCK_EXCEPTION_PATH", 1);
        assertExactPlaceOfBug("UnreleasedLock", "doSomething", "UL_UNRELEASED_LOCK_EXCEPTION_PATH");
    }

    private void assertAnalyzedClassHasNoBugs() {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }

    private void assertAnalyzedClassHasBug(String bugType, int numberOfBugs) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .build();
        assertThat(getBugCollection(), containsExactly(numberOfBugs, bugTypeMatcher));
    }

    private void assertExactPlaceOfBug(String className, String methodName, String bugType) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inClass(className)
                .inMethod(methodName)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

}
