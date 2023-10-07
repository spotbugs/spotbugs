package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class FindSynchronizationLockTest extends AbstractIntegrationTest {
    private static final String BUG_TYPE = "PFL_SYNCHRONIZE_WITH_PRIVATE_FINAL_LOCK_OBJECTS";
    @Test
    public void testBadMethodSynchronizationLock() {
        performAnalysis("instanceLockOnSharedStaticData/LCK00/BadMethodSynchronizationLock.class",
                "instanceLockOnSharedStaticData/LCK00/SomeClass.class");

        assertNumOfBugs(0);

        assertBug(BUG_TYPE);
    }

    private void assertBug(String bugType) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder().bugType(bugType).build();

        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
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
        performAnalysis("instanceLockOnSharedStaticData/LCK00/BadSynchronizationWithPublicStaticLock.class",
                "instanceLockOnSharedStaticData/LCK00/SomeOtherClass.class");

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
