package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FindSynchronizationLockTest extends AbstractIntegrationTest {
    private static final String BUG_TYPE = "PFL_SYNCHRONIZE_WITH_PRIVATE_FINAL_LOCK_OBJECTS";

    @Test
    public void testBadMethodSynchronizationLock() {
        performAnalysis("instanceLockOnSharedStaticData/LCK00/BadMethodSynchronizationLock.class",
                "instanceLockOnSharedStaticData/LCK00/ClassExposingItSelf.class",
                "instanceLockOnSharedStaticData/LCK00/ClassExposingACollectionOfItself.class");

        assertNumOfBugs(2);

        assertBugsExactly(2, BUG_TYPE);
    }

    private void assertBugsExactly(int count, String bugType) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder().bugType(bugType).build();

        assertThat(getBugCollection(), containsExactly(count, bugInstanceMatcher));
    }

    private void assertBug(String bugType) {
        assertBugsExactly(1, bugType);
    }

    @Test
    public void testBadSynchronizationWithPublicFinalLockObject() {
        performAnalysis("instanceLockOnSharedStaticData/LCK00/BadSynchronizationWithPublicFinalLock.class");

        assertNumOfBugs(0);
    }

    @Test
    public void testBadSynchronizationWithPubliclyAccessibleNonFinalLockObject() {
        performAnalysis("instanceLockOnSharedStaticData/LCK00/BadSynchronizationWithPubliclyAccessibleNonFinalLock.class");

        assertNumOfBugs(0);
    }

    @Test
    public void testBadSynchronizationWithPublicNonFinalLockObject() {
        performAnalysis("instanceLockOnSharedStaticData/LCK00/BadSynchronizationWithPublicNonFinalLock.class");

        assertNumOfBugs(0);
    }

    @Test
    public void testBadSynchronizationWithPublicStaticLockObject() {
        performAnalysis("instanceLockOnSharedStaticData/LCK00/BadSynchronizationWithPublicStaticLock.class", "instanceLockOnSharedStaticData/LCK00/SomeOtherClass.class");

        assertNumOfBugs(0);
    }

    @Test
    public void testGoodSynchronizationWithPrivatFinalLokcObject() {
        performAnalysis("instanceLockOnSharedStaticData/LCK00/GoodSynchronizationWithPrivateFinalLock.class");

        assertNumOfBugs(0);
    }

    @Test
    public void testGoodSynchronizationWithPrivatStaticLokcObject() {
        performAnalysis("instanceLockOnSharedStaticData/LCK00/GoodSynchronizationWithPrivateStaticLock.class");

        assertNumOfBugs(0);
    }

    private void assertNumOfBugs(int number) {
        assertEquals(number, getBugCollection().getCollection().size());
    }
}
