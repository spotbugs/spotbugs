package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class FindSynchronizationLockTest extends AbstractIntegrationTest {

    @Test
    public void testBadMethodSynchronizationLock() {
        performAnalysis("instanceLockOnSharedStaticData/LCK00/BadMethodSynchronizationLock.class",
                "instanceLockOnSharedStaticData/LCK00/SomeClass.class");

        assertNumOfBugs(0);
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

    private void assertNumOfBugs(int number) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder().build();

        assertTrue(getBugCollection().getCollection().isEmpty());
    }
}
