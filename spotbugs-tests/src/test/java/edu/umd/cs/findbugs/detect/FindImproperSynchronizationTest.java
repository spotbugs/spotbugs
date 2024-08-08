package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class FindImproperSynchronizationTest extends AbstractIntegrationTest {

    private static final String METHOD_BUG = "US_UNSAFE_METHOD_SYNCHRONIZATION";
    private static final String STATIC_METHOD_BUG = "US_UNSAFE_STATIC_METHOD_SYNCHRONIZATION";
    private static final String OBJECT_BUG = "US_UNSAFE_OBJECT_SYNCHRONIZATION";
    private static final String ACCESSIBLE_OBJECT_BUG = "US_UNSAFE_ACCESSIBLE_OBJECT_SYNCHRONIZATION";
    private static final String INHERITABLE_OBJECT_BUG = "US_UNSAFE_INHERITABLE_OBJECT_SYNCHRONIZATION";
    private static final String EXPOSED_LOCK_OBJECT_BUG = "US_UNSAFE_EXPOSED_OBJECT_SYNCHRONIZATION";
    private static final String BAD_BACKING_COLLECTION = "USBC_UNSAFE_SYNCHRONIZATION_WITH_BACKING_COLLECTION";
    private static final String ACCESSIBLE_BACKING_COLLECTION =
            "USBC_UNSAFE_SYNCHRONIZATION_WITH_ACCESSIBLE_BACKING_COLLECTION";
    private static final String INHERITABLE_BACKING_COLLECTION =
            "USBC_UNSAFE_SYNCHRONIZATION_WITH_INHERITABLE_BACKING_COLLECTION";

    /**
     * The following tests are for object synchronization bugs
     */
    @Test
    void testUnsafeMethodSynchronizationLock() {
        performAnalysis("synchronizationLocks/privateFinalLocks/UnsafeMethodSynchronizationWithClassExposingItSelf.class",
                "synchronizationLocks/privateFinalLocks/UnsafeMethodSynchronizationWithClassExposingACollectionOfItself.class",
                "synchronizationLocks/privateFinalLocks/UnsafeMethodSynchronizationWithClassExposingAMapOfItself.class",
                "synchronizationLocks/privateFinalLocks/UnsafeMethodSynchronizationWithClassExposingMixedGenericOfItself.class");

        assertNumOfBugs(5, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSED_LOCK_OBJECT_BUG);
        assertNumOfBugs(0, BAD_BACKING_COLLECTION);
        assertNumOfBugs(0, ACCESSIBLE_BACKING_COLLECTION);
        assertNumOfBugs(0, INHERITABLE_BACKING_COLLECTION);

        assertBugExactly(METHOD_BUG, "synchronizationLocks.privateFinalLocks.UnsafeMethodSynchronizationWithClassExposingItSelf", "doStuff");
        assertBugExactly(METHOD_BUG, "synchronizationLocks.privateFinalLocks.UnsafeMethodSynchronizationWithClassExposingItSelf", "doStuff2");
        assertBugExactly(METHOD_BUG, "synchronizationLocks.privateFinalLocks.UnsafeMethodSynchronizationWithClassExposingACollectionOfItself",
                "doStuff");
        assertBugExactly(METHOD_BUG, "synchronizationLocks.privateFinalLocks.UnsafeMethodSynchronizationWithClassExposingAMapOfItself", "doStuff");
        assertBugExactly(METHOD_BUG, "synchronizationLocks.privateFinalLocks.UnsafeMethodSynchronizationWithClassExposingMixedGenericOfItself",
                "doStuff");
    }

    @Test
    void testUnsafeStaticMethodSynchronizationLock() {
        performAnalysis("synchronizationLocks/privateFinalLocks/UnsafeMethodSynchronizationWithPublicStaticSynchronization.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(1, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSED_LOCK_OBJECT_BUG);
        assertNumOfBugs(0, BAD_BACKING_COLLECTION);
        assertNumOfBugs(0, ACCESSIBLE_BACKING_COLLECTION);
        assertNumOfBugs(0, INHERITABLE_BACKING_COLLECTION);

        this.assertBugExactly(STATIC_METHOD_BUG, "synchronizationLocks.privateFinalLocks.UnsafeMethodSynchronizationWithPublicStaticSynchronization",
                "doStuff");
    }

    @Test
    void testUnsafeMethodSynchronizationWithMixedBugs() {
        performAnalysis("synchronizationLocks/privateFinalLocks/UnsafeMixedMethodSynchronization.class");

        assertNumOfBugs(1, METHOD_BUG);
        assertNumOfBugs(1, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSED_LOCK_OBJECT_BUG);
        assertNumOfBugs(0, BAD_BACKING_COLLECTION);
        assertNumOfBugs(0, ACCESSIBLE_BACKING_COLLECTION);
        assertNumOfBugs(0, INHERITABLE_BACKING_COLLECTION);

        assertBugExactly(METHOD_BUG, "synchronizationLocks.privateFinalLocks.UnsafeMixedMethodSynchronization", "doStuff");
        this.assertBugExactly(STATIC_METHOD_BUG, "synchronizationLocks.privateFinalLocks.UnsafeMixedMethodSynchronization", "doStuff");
    }

    @Test
    void testSafeMethodSynchronizationLock() {
        performAnalysis("synchronizationLocks/privateFinalLocks/SafeMethodSynchronization.class");

        assertNoUnsafeLockBugs();
    }

    /**
     * The following tests are for object synchronization bugs
     */
    @Test
    void testUnsafeSynchronizationExposedLockInDescendant() {
        performAnalysis("synchronizationLocks/privateFinalLocks/UnsafeSynchronizationWithExposedLockInDescendant.class",
                "synchronizationLocks/privateFinalLocks/UnsafeSynchronizationWithExposingLockInDescendant.class",
                "synchronizationLocks/privateFinalLocks/UnsafeSynchronizationWithExposingLockInDescendant2.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(1, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(5, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(6, EXPOSED_LOCK_OBJECT_BUG);
        assertNumOfBugs(0, BAD_BACKING_COLLECTION);
        assertNumOfBugs(0, ACCESSIBLE_BACKING_COLLECTION);
        assertNumOfBugs(0, INHERITABLE_BACKING_COLLECTION);

        assertBugExactly(EXPOSED_LOCK_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposingLockInDescendant",
                "doStuff1" /* in base */,
                "lock1"); /* Exposed by getLock1 */
        assertBugExactly(INHERITABLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposedLockInDescendant",
                "doStuff1",
                "lock1");

        assertBugExactly(EXPOSED_LOCK_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposingLockInDescendant",
                "doStuff2" /* in base */,
                "lock2"); /* Exposed by getLock2 */
        assertBugExactly(INHERITABLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposedLockInDescendant",
                "doStuff2",
                "lock2");

        assertBugExactly(EXPOSED_LOCK_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposingLockInDescendant",
                "doStuff3" /* in base */,
                "lock3"); /* Exposed by updateLock3 */
        assertBugExactly(INHERITABLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposedLockInDescendant",
                "doStuff3",
                "lock3");

        assertBugExactly(EXPOSED_LOCK_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposingLockInDescendant",
                "doStuff4" /* in base */,
                "lock4"); /* Exposed by updateLock4 */
        assertBugExactly(INHERITABLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposedLockInDescendant",
                "doStuff4",
                "lock4");

        assertBugExactly(EXPOSED_LOCK_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposingLockInDescendant",
                "doStuff5" /* in base */,
                "lock5"); /* Exposed by updateLock5 */
        assertBugExactly(INHERITABLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposedLockInDescendant",
                "doStuff5",
                "lock5");

        // More complex cases involving longer chains of inheritance
        assertBugExactly(EXPOSED_LOCK_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposingLockInDescendant2",
                "doStuff6" /* in parent */,
                "lock6"); /* Exposed by getLock6 */
        assertBugExactly(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposingLockInDescendant", "doStuff6", "lock6");
    }

    @Test
    void testUnsafeSynchronizationWithInheritedLocks() {
        performAnalysis("synchronizationLocks/privateFinalLocks/UnsafeSynchronizationWithPotentiallyInheritedProtectedLocks.class",
                "synchronizationLocks/privateFinalLocks/UnsafeSynchronizationWithExposedPackagePrivateLocks.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(8, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSED_LOCK_OBJECT_BUG);
        assertNumOfBugs(0, BAD_BACKING_COLLECTION);
        assertNumOfBugs(0, ACCESSIBLE_BACKING_COLLECTION);
        assertNumOfBugs(0, INHERITABLE_BACKING_COLLECTION);

        /* Protected bugs */
        assertBugExactly(INHERITABLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithPotentiallyInheritedProtectedLocks",
                "doStuff1",
                "lock1");
        assertBugExactly(INHERITABLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithPotentiallyInheritedProtectedLocks",
                "doStuff2",
                "lock2");
        assertBugExactly(INHERITABLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithPotentiallyInheritedProtectedLocks",
                "doStuff3",
                "lock3");
        assertBugExactly(INHERITABLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithPotentiallyInheritedProtectedLocks",
                "doStuff4",
                "lock4");

        /* Package private bugs */
        assertBugExactly(INHERITABLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposedPackagePrivateLocks",
                "doStuff1",
                "lock1");
        assertBugExactly(INHERITABLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposedPackagePrivateLocks",
                "doStuff2",
                "lock2");
        assertBugExactly(INHERITABLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposedPackagePrivateLocks",
                "doStuff3",
                "lock3");
        assertBugExactly(INHERITABLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposedPackagePrivateLocks",
                "doStuff4",
                "lock4");
    }

    @Test
    void testUnsafeSynchronizationWithExposedLockInDeclaringClass() {
        performAnalysis("synchronizationLocks/privateFinalLocks/UnsafeSynchronizationWithLockExposedInDeclaringClass.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(9, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSED_LOCK_OBJECT_BUG);
        assertNumOfBugs(0, BAD_BACKING_COLLECTION);
        assertNumOfBugs(0, ACCESSIBLE_BACKING_COLLECTION);
        assertNumOfBugs(0, INHERITABLE_BACKING_COLLECTION);

        /* Protected lock bugs */
        assertBugExactly(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithLockExposedInDeclaringClass",
                "doStuff1",
                "lock1"); /* Exposed by getLock1 */
        assertBugExactly(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithLockExposedInDeclaringClass",
                "doStuff2",
                "lock2"); /* Exposed by getLock2 */
        assertBugExactly(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithLockExposedInDeclaringClass",
                "doStuff3",
                "lock3"); /* Exposed by updateLock3 */
        assertBugExactly(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithLockExposedInDeclaringClass",
                "doStuff4",
                "lock4"); /* Exposed by updateLock4 */
        assertBugExactly(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithLockExposedInDeclaringClass",
                "doStuff5",
                "lock5"); /* Exposed by updateLock5 */

        /* Private lock bugs */
        assertBugExactly(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithLockExposedInDeclaringClass",
                "doStuff6",
                "lock6"); /* Exposed by getLock6 */
        assertBugExactly(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithLockExposedInDeclaringClass",
                "doStuff7",
                "lock7"); /* Exposed by updateLock7 */
        assertBugExactly(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithLockExposedInDeclaringClass",
                "doStuff8",
                "lock8"); /* Exposed by updateLock8 */
        assertBugExactly(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithLockExposedInDeclaringClass",
                "doStuff9",
                "lock9"); /* Exposed by updateLock9 */
    }

    @Test
    void testUnsafeSynchronizationWithExposedLockInHierarchy() {
        performAnalysis("synchronizationLocks/privateFinalLocks/UnsafeSynchronizationWithExposedLockFromHierarchy.class",
                "synchronizationLocks/privateFinalLocks/UnsafeSynchronizationUsingExposedLockFromHierarchy.class",
                "synchronizationLocks/privateFinalLocks/UnsafeSynchronizationUsingExposedLockFromHierarchy2.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(6, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSED_LOCK_OBJECT_BUG);
        assertNumOfBugs(0, BAD_BACKING_COLLECTION);
        assertNumOfBugs(0, ACCESSIBLE_BACKING_COLLECTION);
        assertNumOfBugs(0, INHERITABLE_BACKING_COLLECTION);

        assertBugExactly(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationUsingExposedLockFromHierarchy",
                "doStuff1",
                "lock1"); /* Exposed by getLock1 in parent */
        assertBugExactly(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationUsingExposedLockFromHierarchy",
                "doStuff2",
                "lock2"); /* Exposed by getLock2 in parent */
        assertBugExactly(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationUsingExposedLockFromHierarchy",
                "doStuff3",
                "lock3"); /* Exposed by updateLock3 in parent */
        assertBugExactly(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationUsingExposedLockFromHierarchy",
                "doStuff4",
                "lock4"); /* Exposed by updateLock4 in parent */
        assertBugExactly(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationUsingExposedLockFromHierarchy",
                "doStuff5",
                "lock5"); /* Exposed by updateLock5 in parent */

        // More complex cases involving longer chains of inheritance
        assertBugExactly(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationUsingExposedLockFromHierarchy2",
                "doStuff6",
                "lock6"); /* Exposed by getLock6 in parent */
    }

    @Test
    void testUnsafeSynchronizationByExposingLockFromHierarchy() {
        performAnalysis("synchronizationLocks/privateFinalLocks/UnsafeSynchronizationWithExposingLockFromHierarchy.class",
                "synchronizationLocks/privateFinalLocks/UnsafeSynchronizationByExposingLockFromHierarchy.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(5, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSED_LOCK_OBJECT_BUG);
        assertNumOfBugs(0, BAD_BACKING_COLLECTION);
        assertNumOfBugs(0, ACCESSIBLE_BACKING_COLLECTION);
        assertNumOfBugs(0, INHERITABLE_BACKING_COLLECTION);

        assertBugExactly(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationByExposingLockFromHierarchy", "doStuff1",
                "lock1"); /* Exposed by getLock1 */
        assertBugExactly(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationByExposingLockFromHierarchy", "doStuff2",
                "lock2"); /* Exposed by getLock1 */
        assertBugExactly(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationByExposingLockFromHierarchy", "doStuff3",
                "lock3"); /* Exposed by updateLock3 */
        assertBugExactly(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationByExposingLockFromHierarchy", "doStuff4",
                "lock4"); /* Exposed by updateLock4 */
        assertBugExactly(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationByExposingLockFromHierarchy", "doStuff5",
                "lock5"); /* Exposed by updateLock5 */
    }

    @Test
    void testUnsafeSynchronizationWithPublicLocksInPlace() {
        performAnalysis("synchronizationLocks/privateFinalLocks/UnsafeSynchronizationWithPublicLocksInPlace.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(6, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSED_LOCK_OBJECT_BUG);
        assertNumOfBugs(0, BAD_BACKING_COLLECTION);
        assertNumOfBugs(0, ACCESSIBLE_BACKING_COLLECTION);
        assertNumOfBugs(0, INHERITABLE_BACKING_COLLECTION);

        assertBugExactly(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithPublicLocksInPlace", "doStuff1", "lock1");
        assertBugExactly(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithPublicLocksInPlace", "doStuff1Again", "lock1");
        assertBugExactly(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithPublicLocksInPlace", "doStuff2", "lock2");
        assertBugExactly(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithPublicLocksInPlace", "doStuff3", "lock3");
        assertBugExactly(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithPublicLocksInPlace", "doStuff4", "lock4");
        assertBugExactly(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithPublicLocksInPlace", "doStuff5", "lock5");
    }

    @Test
    void testUnsafeSynchronizationWithVisibleLocksExposedWithPolymorphism() {
        performAnalysis("synchronizationLocks/privateFinalLocks/UnsafeSynchronizationWithVisibleLocksExposedWithPolymorphism.class",
                "synchronizationLocks/privateFinalLocks/UnsafeSynchronizationWithVisibleLocksFromHierarchy.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(14, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSED_LOCK_OBJECT_BUG);
        assertNumOfBugs(0, BAD_BACKING_COLLECTION);
        assertNumOfBugs(0, ACCESSIBLE_BACKING_COLLECTION);
        assertNumOfBugs(0, INHERITABLE_BACKING_COLLECTION);

        // Public locks
        assertBugExactly(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff1",
                "lock1");
        assertBugExactly(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff1Again",
                "lock1");
        assertBugExactly(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff2",
                "lock2");
        assertBugExactly(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff3",
                "lock3");
        assertBugExactly(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff4",
                "lock4");
        assertBugExactly(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff5",
                "lock5");

        // Protected locks - it is important that the declaring class is NOT final
        assertBugExactly(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff6",
                "lock6");
        assertBugExactly(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff7",
                "lock7");
        assertBugExactly(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff8",
                "lock8");
        assertBugExactly(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff9",
                "lock9");

        // Package private locks
        assertBugExactly(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff10",
                "lock10");
        assertBugExactly(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff11",
                "lock11");
        assertBugExactly(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff12",
                "lock12");
        assertBugExactly(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff13",
                "lock13");
    }

    @Test
    void testSafeSynchronizationWithPrivateFinalLockObject() {
        performAnalysis("synchronizationLocks/privateFinalLocks/SafeSynchronizationWithPrivateFinalLock.class");

        assertNoUnsafeLockBugs();
    }

    @Test
    void testSafeSynchronizationWithProtectedLockInFinalClass() {
        performAnalysis("synchronizationLocks/privateFinalLocks/SafeSynchronizationWithProtectedLockInFinalClass.class");

        assertNoUnsafeLockBugs();
    }

    @Test
    void testSafeSynchronizationWithInheritance() {
        performAnalysis("synchronizationLocks/privateFinalLocks/SafeSynchronizationWithInheritance.class",
                "synchronizationLocks/privateFinalLocks/SafeSynchronizationInheritedFromParent.class");

        assertNoUnsafeLockBugs();
    }

    @Test
    void testSafeSynchronizationWithBuiltinMethodsSeemToExposeLock() {
        performAnalysis("synchronizationLocks/privateFinalLocks/SafeSynchronizationWithBuiltinMethodExposingLock.class");

        assertNoUnsafeLockBugs();
    }


    private void assertNoUnsafeLockBugs() {
        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(0, BAD_BACKING_COLLECTION);
        assertNumOfBugs(0, ACCESSIBLE_BACKING_COLLECTION);
        assertNumOfBugs(0, INHERITABLE_BACKING_COLLECTION);
    }

    private void assertNumOfBugs(int number, String bugType) {
        final BugInstanceMatcher bugInstance = new BugInstanceMatcherBuilder().bugType(bugType).build();
        assertThat(getBugCollection(), containsExactly(number, bugInstance));
    }

    private void assertBugExactly(String bugType, String clazz, String method) {
        final BugInstanceMatcher bugInstance = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inClass(clazz)
                .inMethod(method)
                .build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

    private void assertBugExactly(String bugType, String clazz, String method, String field) {
        final BugInstanceMatcher bugInstance = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inClass(clazz)
                .inMethod(method)
                .atField(field)
                .build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

}
