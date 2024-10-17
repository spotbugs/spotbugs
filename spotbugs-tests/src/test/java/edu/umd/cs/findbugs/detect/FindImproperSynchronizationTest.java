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

        assertBugTypeCount(METHOD_BUG, 5);
        assertBugTypeCount(STATIC_METHOD_BUG, 0);
        assertBugTypeCount(OBJECT_BUG, 0);
        assertBugTypeCount(ACCESSIBLE_OBJECT_BUG, 0);
        assertBugTypeCount(INHERITABLE_OBJECT_BUG, 0);
        assertBugTypeCount(EXPOSED_LOCK_OBJECT_BUG, 0);
        assertBugTypeCount(BAD_BACKING_COLLECTION, 0);
        assertBugTypeCount(ACCESSIBLE_BACKING_COLLECTION, 0);
        assertBugTypeCount(INHERITABLE_BACKING_COLLECTION, 0);

        assertBugInMethod(METHOD_BUG, "synchronizationLocks.privateFinalLocks.UnsafeMethodSynchronizationWithClassExposingItSelf", "doStuff");
        assertBugInMethod(METHOD_BUG, "synchronizationLocks.privateFinalLocks.UnsafeMethodSynchronizationWithClassExposingItSelf", "doStuff2");
        assertBugInMethod(METHOD_BUG, "synchronizationLocks.privateFinalLocks.UnsafeMethodSynchronizationWithClassExposingACollectionOfItself",
                "doStuff");
        assertBugInMethod(METHOD_BUG, "synchronizationLocks.privateFinalLocks.UnsafeMethodSynchronizationWithClassExposingAMapOfItself",
                "doStuff");
        assertBugInMethod(METHOD_BUG, "synchronizationLocks.privateFinalLocks.UnsafeMethodSynchronizationWithClassExposingMixedGenericOfItself",
                "doStuff");
    }

    @Test
    void testUnsafeStaticMethodSynchronizationLock() {
        performAnalysis("synchronizationLocks/privateFinalLocks/UnsafeMethodSynchronizationWithPublicStaticSynchronization.class");

        assertBugTypeCount(METHOD_BUG, 0);
        assertBugTypeCount(STATIC_METHOD_BUG, 1);
        assertBugTypeCount(OBJECT_BUG, 0);
        assertBugTypeCount(ACCESSIBLE_OBJECT_BUG, 0);
        assertBugTypeCount(INHERITABLE_OBJECT_BUG, 0);
        assertBugTypeCount(EXPOSED_LOCK_OBJECT_BUG, 0);
        assertBugTypeCount(BAD_BACKING_COLLECTION, 0);
        assertBugTypeCount(ACCESSIBLE_BACKING_COLLECTION, 0);
        assertBugTypeCount(INHERITABLE_BACKING_COLLECTION, 0);

        this.assertBugInMethod(STATIC_METHOD_BUG,
                "synchronizationLocks.privateFinalLocks.UnsafeMethodSynchronizationWithPublicStaticSynchronization",
                "doStuff");
    }

    @Test
    void testUnsafeMethodSynchronizationWithMixedBugs() {
        performAnalysis("synchronizationLocks/privateFinalLocks/UnsafeMixedMethodSynchronization.class");

        assertBugTypeCount(METHOD_BUG, 1);
        assertBugTypeCount(STATIC_METHOD_BUG, 1);
        assertBugTypeCount(OBJECT_BUG, 0);
        assertBugTypeCount(ACCESSIBLE_OBJECT_BUG, 0);
        assertBugTypeCount(INHERITABLE_OBJECT_BUG, 0);
        assertBugTypeCount(EXPOSED_LOCK_OBJECT_BUG, 0);
        assertBugTypeCount(BAD_BACKING_COLLECTION, 0);
        assertBugTypeCount(ACCESSIBLE_BACKING_COLLECTION, 0);
        assertBugTypeCount(INHERITABLE_BACKING_COLLECTION, 0);

        assertBugInMethod(METHOD_BUG, "synchronizationLocks.privateFinalLocks.UnsafeMixedMethodSynchronization", "doStuff");
        this.assertBugInMethod(STATIC_METHOD_BUG, "synchronizationLocks.privateFinalLocks.UnsafeMixedMethodSynchronization", "doStuff");
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

        assertBugTypeCount(METHOD_BUG, 0);
        assertBugTypeCount(STATIC_METHOD_BUG, 0);
        assertBugTypeCount(OBJECT_BUG, 1);
        assertBugTypeCount(ACCESSIBLE_OBJECT_BUG, 0);
        assertBugTypeCount(INHERITABLE_OBJECT_BUG, 5);
        assertBugTypeCount(EXPOSED_LOCK_OBJECT_BUG, 6);
        assertBugTypeCount(BAD_BACKING_COLLECTION, 0);
        assertBugTypeCount(ACCESSIBLE_BACKING_COLLECTION, 0);
        assertBugTypeCount(INHERITABLE_BACKING_COLLECTION, 0);

        assertBugInMethodAtField(EXPOSED_LOCK_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposingLockInDescendant",
                "doStuff1" /* in base */,
                "lock1"); /* Exposed by getLock1 */
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposedLockInDescendant",
                "doStuff1",
                "lock1");

        assertBugInMethodAtField(EXPOSED_LOCK_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposingLockInDescendant",
                "doStuff2" /* in base */,
                "lock2"); /* Exposed by getLock2 */
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposedLockInDescendant",
                "doStuff2",
                "lock2");

        assertBugInMethodAtField(EXPOSED_LOCK_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposingLockInDescendant",
                "doStuff3" /* in base */,
                "lock3"); /* Exposed by updateLock3 */
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposedLockInDescendant",
                "doStuff3",
                "lock3");

        assertBugInMethodAtField(EXPOSED_LOCK_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposingLockInDescendant",
                "doStuff4" /* in base */,
                "lock4"); /* Exposed by updateLock4 */
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposedLockInDescendant",
                "doStuff4",
                "lock4");

        assertBugInMethodAtField(EXPOSED_LOCK_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposingLockInDescendant",
                "doStuff5" /* in base */,
                "lock5"); /* Exposed by updateLock5 */
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposedLockInDescendant",
                "doStuff5",
                "lock5");

        // More complex cases involving longer chains of inheritance
        assertBugInMethodAtField(EXPOSED_LOCK_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposingLockInDescendant2",
                "doStuff6" /* in parent */,
                "lock6"); /* Exposed by getLock6 */
        assertBugInMethodAtField(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposingLockInDescendant", "doStuff6", "lock6");
    }

    @Test
    void testUnsafeSynchronizationWithInheritedLocks() {
        performAnalysis("synchronizationLocks/privateFinalLocks/UnsafeSynchronizationWithPotentiallyInheritedProtectedLocks.class",
                "synchronizationLocks/privateFinalLocks/UnsafeSynchronizationWithExposedPackagePrivateLocks.class");

        assertBugTypeCount(METHOD_BUG, 0);
        assertBugTypeCount(STATIC_METHOD_BUG, 0);
        assertBugTypeCount(OBJECT_BUG, 0);
        assertBugTypeCount(ACCESSIBLE_OBJECT_BUG, 0);
        assertBugTypeCount(INHERITABLE_OBJECT_BUG, 8);
        assertBugTypeCount(EXPOSED_LOCK_OBJECT_BUG, 0);
        assertBugTypeCount(BAD_BACKING_COLLECTION, 0);
        assertBugTypeCount(ACCESSIBLE_BACKING_COLLECTION, 0);
        assertBugTypeCount(INHERITABLE_BACKING_COLLECTION, 0);

        /* Protected bugs */
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithPotentiallyInheritedProtectedLocks",
                "doStuff1",
                "lock1");
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithPotentiallyInheritedProtectedLocks",
                "doStuff2",
                "lock2");
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithPotentiallyInheritedProtectedLocks",
                "doStuff3",
                "lock3");
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithPotentiallyInheritedProtectedLocks",
                "doStuff4",
                "lock4");

        /* Package private bugs */
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposedPackagePrivateLocks",
                "doStuff1",
                "lock1");
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposedPackagePrivateLocks",
                "doStuff2",
                "lock2");
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposedPackagePrivateLocks",
                "doStuff3",
                "lock3");
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithExposedPackagePrivateLocks",
                "doStuff4",
                "lock4");
    }

    @Test
    void testUnsafeSynchronizationWithExposedLockInDeclaringClass() {
        performAnalysis("synchronizationLocks/privateFinalLocks/UnsafeSynchronizationWithLockExposedInDeclaringClass.class");

        assertBugTypeCount(METHOD_BUG, 0);
        assertBugTypeCount(STATIC_METHOD_BUG, 0);
        assertBugTypeCount(OBJECT_BUG, 0);
        assertBugTypeCount(ACCESSIBLE_OBJECT_BUG, 9);
        assertBugTypeCount(INHERITABLE_OBJECT_BUG, 0);
        assertBugTypeCount(EXPOSED_LOCK_OBJECT_BUG, 0);
        assertBugTypeCount(BAD_BACKING_COLLECTION, 0);
        assertBugTypeCount(ACCESSIBLE_BACKING_COLLECTION, 0);
        assertBugTypeCount(INHERITABLE_BACKING_COLLECTION, 0);

        /* Protected lock bugs */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithLockExposedInDeclaringClass",
                "doStuff1",
                "lock1"); /* Exposed by getLock1 */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithLockExposedInDeclaringClass",
                "doStuff2",
                "lock2"); /* Exposed by getLock2 */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithLockExposedInDeclaringClass",
                "doStuff3",
                "lock3"); /* Exposed by updateLock3 */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithLockExposedInDeclaringClass",
                "doStuff4",
                "lock4"); /* Exposed by updateLock4 */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithLockExposedInDeclaringClass",
                "doStuff5",
                "lock5"); /* Exposed by updateLock5 */

        /* Private lock bugs */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithLockExposedInDeclaringClass",
                "doStuff6",
                "lock6"); /* Exposed by getLock6 */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithLockExposedInDeclaringClass",
                "doStuff7",
                "lock7"); /* Exposed by updateLock7 */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithLockExposedInDeclaringClass",
                "doStuff8",
                "lock8"); /* Exposed by updateLock8 */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithLockExposedInDeclaringClass",
                "doStuff9",
                "lock9"); /* Exposed by updateLock9 */
    }

    @Test
    void testUnsafeSynchronizationWithExposedLockInHierarchy() {
        performAnalysis("synchronizationLocks/privateFinalLocks/UnsafeSynchronizationWithExposedLockFromHierarchy.class",
                "synchronizationLocks/privateFinalLocks/UnsafeSynchronizationUsingExposedLockFromHierarchy.class",
                "synchronizationLocks/privateFinalLocks/UnsafeSynchronizationUsingExposedLockFromHierarchy2.class");

        assertBugTypeCount(METHOD_BUG, 0);
        assertBugTypeCount(STATIC_METHOD_BUG, 0);
        assertBugTypeCount(OBJECT_BUG, 0);
        assertBugTypeCount(ACCESSIBLE_OBJECT_BUG, 6);
        assertBugTypeCount(INHERITABLE_OBJECT_BUG, 0);
        assertBugTypeCount(EXPOSED_LOCK_OBJECT_BUG, 0);
        assertBugTypeCount(BAD_BACKING_COLLECTION, 0);
        assertBugTypeCount(ACCESSIBLE_BACKING_COLLECTION, 0);
        assertBugTypeCount(INHERITABLE_BACKING_COLLECTION, 0);

        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationUsingExposedLockFromHierarchy",
                "doStuff1",
                "lock1"); /* Exposed by getLock1 in parent */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationUsingExposedLockFromHierarchy",
                "doStuff2",
                "lock2"); /* Exposed by getLock2 in parent */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationUsingExposedLockFromHierarchy",
                "doStuff3",
                "lock3"); /* Exposed by updateLock3 in parent */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationUsingExposedLockFromHierarchy",
                "doStuff4",
                "lock4"); /* Exposed by updateLock4 in parent */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationUsingExposedLockFromHierarchy",
                "doStuff5",
                "lock5"); /* Exposed by updateLock5 in parent */

        // More complex cases involving longer chains of inheritance
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationUsingExposedLockFromHierarchy2",
                "doStuff6",
                "lock6"); /* Exposed by getLock6 in parent */
    }

    @Test
    void testUnsafeSynchronizationByExposingLockFromHierarchy() {
        performAnalysis("synchronizationLocks/privateFinalLocks/UnsafeSynchronizationWithExposingLockFromHierarchy.class",
                "synchronizationLocks/privateFinalLocks/UnsafeSynchronizationByExposingLockFromHierarchy.class");

        assertBugTypeCount(METHOD_BUG, 0);
        assertBugTypeCount(STATIC_METHOD_BUG, 0);
        assertBugTypeCount(OBJECT_BUG, 0);
        assertBugTypeCount(ACCESSIBLE_OBJECT_BUG, 5);
        assertBugTypeCount(INHERITABLE_OBJECT_BUG, 0);
        assertBugTypeCount(EXPOSED_LOCK_OBJECT_BUG, 0);
        assertBugTypeCount(BAD_BACKING_COLLECTION, 0);
        assertBugTypeCount(ACCESSIBLE_BACKING_COLLECTION, 0);
        assertBugTypeCount(INHERITABLE_BACKING_COLLECTION, 0);

        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationByExposingLockFromHierarchy", "doStuff1",
                "lock1"); /* Exposed by getLock1 */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationByExposingLockFromHierarchy", "doStuff2",
                "lock2"); /* Exposed by getLock1 */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationByExposingLockFromHierarchy", "doStuff3",
                "lock3"); /* Exposed by updateLock3 */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationByExposingLockFromHierarchy", "doStuff4",
                "lock4"); /* Exposed by updateLock4 */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationByExposingLockFromHierarchy", "doStuff5",
                "lock5"); /* Exposed by updateLock5 */
    }

    @Test
    void testUnsafeSynchronizationWithPublicLocksInPlace() {
        performAnalysis("synchronizationLocks/privateFinalLocks/UnsafeSynchronizationWithPublicLocksInPlace.class");

        assertBugTypeCount(METHOD_BUG, 0);
        assertBugTypeCount(STATIC_METHOD_BUG, 0);
        assertBugTypeCount(OBJECT_BUG, 6);
        assertBugTypeCount(ACCESSIBLE_OBJECT_BUG, 0);
        assertBugTypeCount(INHERITABLE_OBJECT_BUG, 0);
        assertBugTypeCount(EXPOSED_LOCK_OBJECT_BUG, 0);
        assertBugTypeCount(BAD_BACKING_COLLECTION, 0);
        assertBugTypeCount(ACCESSIBLE_BACKING_COLLECTION, 0);
        assertBugTypeCount(INHERITABLE_BACKING_COLLECTION, 0);

        assertBugInMethodAtField(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithPublicLocksInPlace", "doStuff1", "lock1");
        assertBugInMethodAtField(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithPublicLocksInPlace", "doStuff1Again", "lock1");
        assertBugInMethodAtField(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithPublicLocksInPlace", "doStuff2", "lock2");
        assertBugInMethodAtField(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithPublicLocksInPlace", "doStuff3", "lock3");
        assertBugInMethodAtField(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithPublicLocksInPlace", "doStuff4", "lock4");
        assertBugInMethodAtField(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithPublicLocksInPlace", "doStuff5", "lock5");
    }

    @Test
    void testUnsafeSynchronizationWithVisibleLocksExposedWithPolymorphism() {
        performAnalysis("synchronizationLocks/privateFinalLocks/UnsafeSynchronizationWithVisibleLocksExposedWithPolymorphism.class",
                "synchronizationLocks/privateFinalLocks/UnsafeSynchronizationWithVisibleLocksFromHierarchy.class");

        assertBugTypeCount(METHOD_BUG, 0);
        assertBugTypeCount(STATIC_METHOD_BUG, 0);
        assertBugTypeCount(OBJECT_BUG, 14);
        assertBugTypeCount(ACCESSIBLE_OBJECT_BUG, 0);
        assertBugTypeCount(INHERITABLE_OBJECT_BUG, 0);
        assertBugTypeCount(EXPOSED_LOCK_OBJECT_BUG, 0);
        assertBugTypeCount(BAD_BACKING_COLLECTION, 0);
        assertBugTypeCount(ACCESSIBLE_BACKING_COLLECTION, 0);
        assertBugTypeCount(INHERITABLE_BACKING_COLLECTION, 0);

        // Public locks
        assertBugInMethodAtField(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff1",
                "lock1");
        assertBugInMethodAtField(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff1Again",
                "lock1");
        assertBugInMethodAtField(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff2",
                "lock2");
        assertBugInMethodAtField(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff3",
                "lock3");
        assertBugInMethodAtField(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff4",
                "lock4");
        assertBugInMethodAtField(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff5",
                "lock5");

        // Protected locks - it is important that the declaring class is NOT final
        assertBugInMethodAtField(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff6",
                "lock6");
        assertBugInMethodAtField(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff7",
                "lock7");
        assertBugInMethodAtField(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff8",
                "lock8");
        assertBugInMethodAtField(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff9",
                "lock9");

        // Package private locks
        assertBugInMethodAtField(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff10",
                "lock10");
        assertBugInMethodAtField(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff11",
                "lock11");
        assertBugInMethodAtField(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff12",
                "lock12");
        assertBugInMethodAtField(OBJECT_BUG, "synchronizationLocks.privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff13",
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
        assertBugTypeCount(METHOD_BUG, 0);
        assertBugTypeCount(STATIC_METHOD_BUG, 0);
        assertBugTypeCount(OBJECT_BUG, 0);
        assertBugTypeCount(ACCESSIBLE_OBJECT_BUG, 0);
        assertBugTypeCount(INHERITABLE_OBJECT_BUG, 0);
        assertBugTypeCount(BAD_BACKING_COLLECTION, 0);
        assertBugTypeCount(ACCESSIBLE_BACKING_COLLECTION, 0);
        assertBugTypeCount(INHERITABLE_BACKING_COLLECTION, 0);
    }

}
