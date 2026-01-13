package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


class FindImproperSynchronizationTest extends AbstractIntegrationTest {

    private static final String METHOD_BUG = "USO_UNSAFE_METHOD_SYNCHRONIZATION";
    private static final String STATIC_METHOD_BUG = "USO_UNSAFE_STATIC_METHOD_SYNCHRONIZATION";
    private static final String OBJECT_BUG = "USO_UNSAFE_OBJECT_SYNCHRONIZATION";
    private static final String ACCESSIBLE_OBJECT_BUG = "USO_UNSAFE_ACCESSIBLE_OBJECT_SYNCHRONIZATION";
    private static final String INHERITABLE_OBJECT_BUG = "USO_UNSAFE_INHERITABLE_OBJECT_SYNCHRONIZATION";
    private static final String EXPOSED_LOCK_OBJECT_BUG = "USO_UNSAFE_EXPOSED_OBJECT_SYNCHRONIZATION";
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

        assertBugInMethod(METHOD_BUG, "UnsafeMethodSynchronizationWithClassExposingItSelf", "doStuff");
        assertBugInMethod(METHOD_BUG, "UnsafeMethodSynchronizationWithClassExposingItSelf", "doStuff2");
        assertBugInMethod(METHOD_BUG, "UnsafeMethodSynchronizationWithClassExposingACollectionOfItself", "doStuff");
        assertBugInMethod(METHOD_BUG, "UnsafeMethodSynchronizationWithClassExposingAMapOfItself", "doStuff");
        assertBugInMethod(METHOD_BUG, "UnsafeMethodSynchronizationWithClassExposingMixedGenericOfItself", "doStuff");
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

        this.assertBugInMethod(STATIC_METHOD_BUG, "UnsafeMethodSynchronizationWithPublicStaticSynchronization", "doStuff");
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

        assertBugInMethod(METHOD_BUG, "UnsafeMixedMethodSynchronization", "doStuff");
        assertBugInMethod(STATIC_METHOD_BUG, "UnsafeMixedMethodSynchronization", "doStuff");
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

        assertBugInMethodAtField(EXPOSED_LOCK_OBJECT_BUG,
                "UnsafeSynchronizationWithExposingLockInDescendant",
                "doStuff1" /* in base */,
                "lock1"); /* Exposed by getLock1 */
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG,
                "UnsafeSynchronizationWithExposedLockInDescendant",
                "doStuff1",
                "lock1");

        assertBugInMethodAtField(EXPOSED_LOCK_OBJECT_BUG,
                "UnsafeSynchronizationWithExposingLockInDescendant",
                "doStuff2" /* in base */,
                "lock2"); /* Exposed by getLock2 */
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG,
                "UnsafeSynchronizationWithExposedLockInDescendant",
                "doStuff2",
                "lock2");

        assertBugInMethodAtField(EXPOSED_LOCK_OBJECT_BUG,
                "UnsafeSynchronizationWithExposingLockInDescendant",
                "doStuff3" /* in base */,
                "lock3"); /* Exposed by updateLock3 */
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG,
                "UnsafeSynchronizationWithExposedLockInDescendant",
                "doStuff3",
                "lock3");

        assertBugInMethodAtField(EXPOSED_LOCK_OBJECT_BUG,
                "UnsafeSynchronizationWithExposingLockInDescendant",
                "doStuff4" /* in base */,
                "lock4"); /* Exposed by updateLock4 */
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG,
                "UnsafeSynchronizationWithExposedLockInDescendant",
                "doStuff4",
                "lock4");

        assertBugInMethodAtField(EXPOSED_LOCK_OBJECT_BUG,
                "UnsafeSynchronizationWithExposingLockInDescendant",
                "doStuff5" /* in base */,
                "lock5"); /* Exposed by updateLock5 */
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG,
                "UnsafeSynchronizationWithExposedLockInDescendant",
                "doStuff5",
                "lock5");

        // More complex cases involving longer chains of inheritance
        assertBugInMethodAtField(EXPOSED_LOCK_OBJECT_BUG,
                "UnsafeSynchronizationWithExposingLockInDescendant2",
                "doStuff6" /* in parent */,
                "lock6"); /* Exposed by getLock6 */
        assertBugInMethodAtField(OBJECT_BUG,
                "UnsafeSynchronizationWithExposingLockInDescendant",
                "doStuff6",
                "lock6");
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
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG,
                "UnsafeSynchronizationWithPotentiallyInheritedProtectedLocks",
                "doStuff1",
                "lock1");
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG,
                "UnsafeSynchronizationWithPotentiallyInheritedProtectedLocks",
                "doStuff2",
                "lock2");
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG,
                "UnsafeSynchronizationWithPotentiallyInheritedProtectedLocks",
                "doStuff3",
                "lock3");
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG,
                "UnsafeSynchronizationWithPotentiallyInheritedProtectedLocks",
                "doStuff4",
                "lock4");

        /* Package private bugs */
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG,
                "UnsafeSynchronizationWithExposedPackagePrivateLocks",
                "doStuff1",
                "lock1");
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG,
                "UnsafeSynchronizationWithExposedPackagePrivateLocks",
                "doStuff2",
                "lock2");
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG,
                "UnsafeSynchronizationWithExposedPackagePrivateLocks",
                "doStuff3",
                "lock3");
        assertBugInMethodAtField(INHERITABLE_OBJECT_BUG,
                "UnsafeSynchronizationWithExposedPackagePrivateLocks",
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
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG,
                "UnsafeSynchronizationWithLockExposedInDeclaringClass",
                "doStuff1",
                "lock1"); /* Exposed by getLock1 */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG,
                "UnsafeSynchronizationWithLockExposedInDeclaringClass",
                "doStuff2",
                "lock2"); /* Exposed by getLock2 */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG,
                "UnsafeSynchronizationWithLockExposedInDeclaringClass",
                "doStuff3",
                "lock3"); /* Exposed by updateLock3 */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG,
                "UnsafeSynchronizationWithLockExposedInDeclaringClass",
                "doStuff4",
                "lock4"); /* Exposed by updateLock4 */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG,
                "UnsafeSynchronizationWithLockExposedInDeclaringClass",
                "doStuff5",
                "lock5"); /* Exposed by updateLock5 */

        /* Private lock bugs */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG,
                "UnsafeSynchronizationWithLockExposedInDeclaringClass",
                "doStuff6",
                "lock6"); /* Exposed by getLock6 */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG,
                "UnsafeSynchronizationWithLockExposedInDeclaringClass",
                "doStuff7",
                "lock7"); /* Exposed by updateLock7 */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG,
                "UnsafeSynchronizationWithLockExposedInDeclaringClass",
                "doStuff8",
                "lock8"); /* Exposed by updateLock8 */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG,
                "UnsafeSynchronizationWithLockExposedInDeclaringClass",
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

        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG,
                "UnsafeSynchronizationUsingExposedLockFromHierarchy",
                "doStuff1",
                "lock1"); /* Exposed by getLock1 in parent */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG,
                "UnsafeSynchronizationUsingExposedLockFromHierarchy",
                "doStuff2",
                "lock2"); /* Exposed by getLock2 in parent */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG,
                "UnsafeSynchronizationUsingExposedLockFromHierarchy",
                "doStuff3",
                "lock3"); /* Exposed by updateLock3 in parent */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG,
                "UnsafeSynchronizationUsingExposedLockFromHierarchy",
                "doStuff4",
                "lock4"); /* Exposed by updateLock4 in parent */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG,
                "UnsafeSynchronizationUsingExposedLockFromHierarchy",
                "doStuff5",
                "lock5"); /* Exposed by updateLock5 in parent */

        // More complex cases involving longer chains of inheritance
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG,
                "UnsafeSynchronizationUsingExposedLockFromHierarchy2",
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

        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG,
                "UnsafeSynchronizationByExposingLockFromHierarchy",
                "doStuff1",
                "lock1"); /* Exposed by getLock1 */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG,
                "UnsafeSynchronizationByExposingLockFromHierarchy",
                "doStuff2",
                "lock2"); /* Exposed by getLock1 */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG,
                "UnsafeSynchronizationByExposingLockFromHierarchy",
                "doStuff3",
                "lock3"); /* Exposed by updateLock3 */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG,
                "UnsafeSynchronizationByExposingLockFromHierarchy",
                "doStuff4",
                "lock4"); /* Exposed by updateLock4 */
        assertBugInMethodAtField(ACCESSIBLE_OBJECT_BUG,
                "UnsafeSynchronizationByExposingLockFromHierarchy",
                "doStuff5",
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

        assertBugInMethodAtField(OBJECT_BUG,
                "UnsafeSynchronizationWithPublicLocksInPlace",
                "doStuff1",
                "lock1");
        assertBugInMethodAtField(OBJECT_BUG,
                "UnsafeSynchronizationWithPublicLocksInPlace",
                "doStuff1Again",
                "lock1");
        assertBugInMethodAtField(OBJECT_BUG,
                "UnsafeSynchronizationWithPublicLocksInPlace",
                "doStuff2",
                "lock2");
        assertBugInMethodAtField(OBJECT_BUG,
                "UnsafeSynchronizationWithPublicLocksInPlace",
                "doStuff3",
                "lock3");
        assertBugInMethodAtField(OBJECT_BUG,
                "UnsafeSynchronizationWithPublicLocksInPlace",
                "doStuff4",
                "lock4");
        assertBugInMethodAtField(OBJECT_BUG,
                "UnsafeSynchronizationWithPublicLocksInPlace",
                "doStuff5",
                "lock5");
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
        assertBugInMethodAtField(OBJECT_BUG,
                "UnsafeSynchronizationWithVisibleLocksFromHierarchy",
                "doStuff1",
                "lock1");
        assertBugInMethodAtField(OBJECT_BUG,
                "UnsafeSynchronizationWithVisibleLocksFromHierarchy",
                "doStuff1Again",
                "lock1");
        assertBugInMethodAtField(OBJECT_BUG,
                "UnsafeSynchronizationWithVisibleLocksFromHierarchy",
                "doStuff2",
                "lock2");
        assertBugInMethodAtField(OBJECT_BUG,
                "UnsafeSynchronizationWithVisibleLocksFromHierarchy",
                "doStuff3",
                "lock3");
        assertBugInMethodAtField(OBJECT_BUG,
                "UnsafeSynchronizationWithVisibleLocksFromHierarchy",
                "doStuff4",
                "lock4");
        assertBugInMethodAtField(OBJECT_BUG,
                "UnsafeSynchronizationWithVisibleLocksFromHierarchy",
                "doStuff5",
                "lock5");

        // Protected locks - it is important that the declaring class is NOT final
        assertBugInMethodAtField(OBJECT_BUG,
                "UnsafeSynchronizationWithVisibleLocksFromHierarchy",
                "doStuff6",
                "lock6");
        assertBugInMethodAtField(OBJECT_BUG,
                "UnsafeSynchronizationWithVisibleLocksFromHierarchy",
                "doStuff7",
                "lock7");
        assertBugInMethodAtField(OBJECT_BUG,
                "UnsafeSynchronizationWithVisibleLocksFromHierarchy",
                "doStuff8",
                "lock8");
        assertBugInMethodAtField(OBJECT_BUG,
                "UnsafeSynchronizationWithVisibleLocksFromHierarchy",
                "doStuff9",
                "lock9");

        // Package private locks
        assertBugInMethodAtField(OBJECT_BUG,
                "UnsafeSynchronizationWithVisibleLocksFromHierarchy",
                "doStuff10",
                "lock10");
        assertBugInMethodAtField(OBJECT_BUG,
                "UnsafeSynchronizationWithVisibleLocksFromHierarchy",
                "doStuff11",
                "lock11");
        assertBugInMethodAtField(OBJECT_BUG,
                "UnsafeSynchronizationWithVisibleLocksFromHierarchy",
                "doStuff12",
                "lock12");
        assertBugInMethodAtField(OBJECT_BUG,
                "UnsafeSynchronizationWithVisibleLocksFromHierarchy",
                "doStuff13",
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
