package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class FindSynchronizationLockTest extends AbstractIntegrationTest {

    private static final String METHOD_BUG = "US_UNSAFE_METHOD_SYNCHRONIZATION";
    private static final String STATIC_METHOD_BUG = "US_UNSAFE_STATIC_METHOD_SYNCHRONIZATION";
    private static final String OBJECT_BUG = "US_UNSAFE_OBJECT_SYNCHRONIZATION";
    private static final String ACCESSIBLE_OBJECT_BUG = "US_UNSAFE_ACCESSIBLE_OBJECT_SYNCHRONIZATION";
    private static final String INHERITABLE_OBJECT_BUG = "US_UNSAFE_INHERITABLE_OBJECT_SYNCHRONIZATION";
    private static final String EXPOSED_LOCK_OBJECT_BUG = "US_UNSAFE_EXPOSED_OBJECT_SYNCHRONIZATION";

    /**
     * The following tests are for object synchronization bugs
     */
    @Test
    void testUnsafeMethodSynchronizationLock() {
        performAnalysis("privateFinalLocks/UnsafeMethodSynchronizationWithClassExposingItSelf.class",
                "privateFinalLocks/UnsafeMethodSynchronizationWithClassExposingACollectionOfItself.class",
                "privateFinalLocks/UnsafeMethodSynchronizationWithClassExposingAMapOfItself.class",
                "privateFinalLocks/UnsafeMethodSynchronizationWithClassExposingMixedGenericOfItself.class");

        assertNumOfBugs(5, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSED_LOCK_OBJECT_BUG);

        assertMethodBugExactly("privateFinalLocks.UnsafeMethodSynchronizationWithClassExposingItSelf", "doStuff");
        assertMethodBugExactly("privateFinalLocks.UnsafeMethodSynchronizationWithClassExposingItSelf", "doStuff2");
        assertMethodBugExactly("privateFinalLocks.UnsafeMethodSynchronizationWithClassExposingACollectionOfItself", "doStuff");
        assertMethodBugExactly("privateFinalLocks.UnsafeMethodSynchronizationWithClassExposingAMapOfItself", "doStuff");
        assertMethodBugExactly("privateFinalLocks.UnsafeMethodSynchronizationWithClassExposingMixedGenericOfItself", "doStuff");
    }

    @Test
    void testUnsafeStaticMethodSynchronizationLock() {
        performAnalysis("privateFinalLocks/UnsafeMethodSynchronizationWithPublicStaticSynchronization.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(1, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSED_LOCK_OBJECT_BUG);

        assertStaticMethodBugExactly("privateFinalLocks.UnsafeMethodSynchronizationWithPublicStaticSynchronization", "doStuff");
    }

    @Test
    void testUnsafeMethodSynchronizationWithMixedBugs() {
        performAnalysis("privateFinalLocks/UnsafeMixedMethodSynchronization.class");

        assertNumOfBugs(1, METHOD_BUG);
        assertNumOfBugs(1, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSED_LOCK_OBJECT_BUG);

        assertMethodBugExactly("privateFinalLocks.UnsafeMixedMethodSynchronization", "doStuff");
        assertStaticMethodBugExactly("privateFinalLocks.UnsafeMixedMethodSynchronization", "doStuff");
    }

    @Test
    void testSafeMethodSynchronizationLock() {
        performAnalysis("privateFinalLocks/SafeMethodSynchronization.class");

        assertNoUnsafeLockBugs();
    }

    /**
     * The following tests are for object synchronization bugs
     */
    @Test
    void testUnsafeSynchronizationExposedLockInDescendant() {
        performAnalysis("privateFinalLocks/UnsafeSynchronizationWithExposedLockInDescendant.class",
                "privateFinalLocks/UnsafeSynchronizationWithExposingLockInDescendant.class",
                "privateFinalLocks/UnsafeSynchronizationWithExposingLockInDescendant2.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(1, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(5, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(6, EXPOSED_LOCK_OBJECT_BUG);

        assertExposedObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithExposingLockInDescendant", "doStuff1" /* in base */,
                "lock1"); /* Exposed by getLock1 */
        assertInheritableObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithExposedLockInDescendant", "doStuff1", "lock1");

        assertExposedObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithExposingLockInDescendant", "doStuff2" /* in base */,
                "lock2"); /* Exposed by getLock2 */
        assertInheritableObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithExposedLockInDescendant", "doStuff2", "lock2");

        assertExposedObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithExposingLockInDescendant", "doStuff3" /* in base */,
                "lock3"); /* Exposed by updateLock3 */
        assertInheritableObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithExposedLockInDescendant", "doStuff3", "lock3");

        assertExposedObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithExposingLockInDescendant", "doStuff4" /* in base */,
                "lock4"); /* Exposed by updateLock4 */
        assertInheritableObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithExposedLockInDescendant", "doStuff4", "lock4");

        assertExposedObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithExposingLockInDescendant", "doStuff5" /* in base */,
                "lock5"); /* Exposed by updateLock5 */
        assertInheritableObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithExposedLockInDescendant", "doStuff5", "lock5");

        // More complex cases involving longer chains of inheritance
        assertExposedObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithExposingLockInDescendant2", "doStuff6" /* in parent */,
                "lock6"); /* Exposed by getLock6 */
        assertObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithExposingLockInDescendant", "doStuff6", "lock6");
    }

    @Test
    void testUnsafeSynchronizationWithInheritedLocks() {
        performAnalysis("privateFinalLocks/UnsafeSynchronizationWithPotentiallyInheritedProtectedLocks.class",
                "privateFinalLocks/UnsafeSynchronizationWithExposedPackagePrivateLocks.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(8, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSED_LOCK_OBJECT_BUG);

        /* Protected bugs */
        assertInheritableObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithPotentiallyInheritedProtectedLocks", "doStuff1", "lock1");
        assertInheritableObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithPotentiallyInheritedProtectedLocks", "doStuff2", "lock2");
        assertInheritableObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithPotentiallyInheritedProtectedLocks", "doStuff3", "lock3");
        assertInheritableObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithPotentiallyInheritedProtectedLocks", "doStuff4", "lock4");

        /* Package private bugs */
        assertInheritableObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithExposedPackagePrivateLocks", "doStuff1", "lock1");
        assertInheritableObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithExposedPackagePrivateLocks", "doStuff2", "lock2");
        assertInheritableObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithExposedPackagePrivateLocks", "doStuff3", "lock3");
        assertInheritableObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithExposedPackagePrivateLocks", "doStuff4", "lock4");
    }

    @Test
    void testUnsafeSynchronizationWithExposedLockInDeclaringClass() {
        performAnalysis("privateFinalLocks/UnsafeSynchronizationWithLockExposedInDeclaringClass.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(9, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSED_LOCK_OBJECT_BUG);

        /* Protected lock bugs */
        assertAccessibleObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithLockExposedInDeclaringClass", "doStuff1",
                "lock1"); /* Exposed by getLock1 */
        assertAccessibleObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithLockExposedInDeclaringClass", "doStuff2",
                "lock2"); /* Exposed by getLock2 */
        assertAccessibleObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithLockExposedInDeclaringClass", "doStuff3",
                "lock3"); /* Exposed by updateLock3 */
        assertAccessibleObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithLockExposedInDeclaringClass", "doStuff4",
                "lock4"); /* Exposed by updateLock4 */
        assertAccessibleObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithLockExposedInDeclaringClass", "doStuff5",
                "lock5"); /* Exposed by updateLock5 */

        /* Private lock bugs */
        assertAccessibleObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithLockExposedInDeclaringClass", "doStuff6",
                "lock6"); /* Exposed by getLock6 */
        assertAccessibleObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithLockExposedInDeclaringClass", "doStuff7",
                "lock7"); /* Exposed by updateLock7 */
        assertAccessibleObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithLockExposedInDeclaringClass", "doStuff8",
                "lock8"); /* Exposed by updateLock8 */
        assertAccessibleObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithLockExposedInDeclaringClass", "doStuff9",
                "lock9"); /* Exposed by updateLock9 */
    }

    @Test
    void testUnsafeSynchronizationWithExposedLockInHierarchy() {
        performAnalysis("privateFinalLocks/UnsafeSynchronizationWithExposedLockFromHierarchy.class",
                "privateFinalLocks/UnsafeSynchronizationUsingExposedLockFromHierarchy.class",
                "privateFinalLocks/UnsafeSynchronizationUsingExposedLockFromHierarchy2.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(6, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSED_LOCK_OBJECT_BUG);

        assertAccessibleObjectBugExactly("privateFinalLocks.UnsafeSynchronizationUsingExposedLockFromHierarchy", "doStuff1",
                "lock1"); /* Exposed by getLock1 in parent */
        assertAccessibleObjectBugExactly("privateFinalLocks.UnsafeSynchronizationUsingExposedLockFromHierarchy", "doStuff2",
                "lock2"); /* Exposed by getLock2 in parent */
        assertAccessibleObjectBugExactly("privateFinalLocks.UnsafeSynchronizationUsingExposedLockFromHierarchy", "doStuff3",
                "lock3"); /* Exposed by updateLock3 in parent */
        assertAccessibleObjectBugExactly("privateFinalLocks.UnsafeSynchronizationUsingExposedLockFromHierarchy", "doStuff4",
                "lock4"); /* Exposed by updateLock4 in parent */
        assertAccessibleObjectBugExactly("privateFinalLocks.UnsafeSynchronizationUsingExposedLockFromHierarchy", "doStuff5",
                "lock5"); /* Exposed by updateLock5 in parent */

        // More complex cases involving longer chains of inheritance
        assertAccessibleObjectBugExactly("privateFinalLocks.UnsafeSynchronizationUsingExposedLockFromHierarchy2", "doStuff6",
                "lock6"); /* Exposed by getLock6 in parent */
    }

    @Test
    void testUnsafeSynchronizationByExposingLockFromHierarchy() {
        performAnalysis("privateFinalLocks/UnsafeSynchronizationWithExposingLockFromHierarchy.class",
                "privateFinalLocks/UnsafeSynchronizationByExposingLockFromHierarchy.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(5, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSED_LOCK_OBJECT_BUG);

        assertAccessibleObjectBugExactly("privateFinalLocks.UnsafeSynchronizationByExposingLockFromHierarchy", "doStuff1",
                "lock1"); /* Exposed by getLock1 */
        assertAccessibleObjectBugExactly("privateFinalLocks.UnsafeSynchronizationByExposingLockFromHierarchy", "doStuff2",
                "lock2"); /* Exposed by getLock1 */
        assertAccessibleObjectBugExactly("privateFinalLocks.UnsafeSynchronizationByExposingLockFromHierarchy", "doStuff3",
                "lock3"); /* Exposed by updateLock3 */
        assertAccessibleObjectBugExactly("privateFinalLocks.UnsafeSynchronizationByExposingLockFromHierarchy", "doStuff4",
                "lock4"); /* Exposed by updateLock4 */
        assertAccessibleObjectBugExactly("privateFinalLocks.UnsafeSynchronizationByExposingLockFromHierarchy", "doStuff5",
                "lock5"); /* Exposed by updateLock5 */
    }

    @Test
    void testUnsafeSynchronizationWithPublicLocksInPlace() {
        performAnalysis("privateFinalLocks/UnsafeSynchronizationWithPublicLocksInPlace.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(6, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSED_LOCK_OBJECT_BUG);

        assertObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithPublicLocksInPlace", "doStuff1", "lock1");
        assertObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithPublicLocksInPlace", "doStuff1Again", "lock1");
        assertObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithPublicLocksInPlace", "doStuff2", "lock2");
        assertObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithPublicLocksInPlace", "doStuff3", "lock3");
        assertObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithPublicLocksInPlace", "doStuff4", "lock4");
        assertObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithPublicLocksInPlace", "doStuff5", "lock5");
    }

    @Test
    void testUnsafeSynchronizationWithVisibleLocksExposedWithPolymorphism() {
        performAnalysis("privateFinalLocks/UnsafeSynchronizationWithVisibleLocksExposedWithPolymorphism.class",
                "privateFinalLocks/UnsafeSynchronizationWithVisibleLocksFromHierarchy.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(14, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSED_LOCK_OBJECT_BUG);

        // Public locks
        assertObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff1", "lock1");
        assertObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff1Again", "lock1");
        assertObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff2", "lock2");
        assertObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff3", "lock3");
        assertObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff4", "lock4");
        assertObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff5", "lock5");

        // Protected locks - it is important that the declaring class is NOT final
        assertObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff6", "lock6");
        assertObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff7", "lock7");
        assertObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff8", "lock8");
        assertObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff9", "lock9");

        // Package private locks
        assertObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff10", "lock10");
        assertObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff11", "lock11");
        assertObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff12", "lock12");
        assertObjectBugExactly("privateFinalLocks.UnsafeSynchronizationWithVisibleLocksFromHierarchy", "doStuff13", "lock13");
    }

    @Test
    void testSafeSynchronizationWithPrivateFinalLockObject() {
        performAnalysis("privateFinalLocks/SafeSynchronizationWithPrivateFinalLock.class");

        assertNoUnsafeLockBugs();
    }

    @Test
    void testSafeSynchronizationWithProtectedLockInFinalClass() {
        performAnalysis("privateFinalLocks/SafeSynchronizationWithProtectedLockInFinalClass.class");

        assertNoUnsafeLockBugs();
    }

    @Test
    void testSafeSynchronizationWithInheritance() {
        performAnalysis("privateFinalLocks/SafeSynchronizationWithInheritance.class",
                "privateFinalLocks/SafeSynchronizationInheritedFromParent.class");

        assertNoUnsafeLockBugs();
    }

    @Test
    void testSafeSynchronizationWithBuiltinMethodsSeemToExposeLock() {
        performAnalysis("privateFinalLocks/SafeSynchronizationWithBuiltinMethodExposingLock.class");

        assertNoUnsafeLockBugs();
    }


    private void assertNoUnsafeLockBugs() {
        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITABLE_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSED_LOCK_OBJECT_BUG);
    }

    private void assertNumOfBugs(int number, String bugType) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().bugType(bugType).build();
        assertThat(getBugCollection(), containsExactly(number, bugTypeMatcher));
    }

    private void assertMethodBugExactly(String clazz, String method) {
        final BugInstanceMatcher bugInstance = new BugInstanceMatcherBuilder()
                .bugType(METHOD_BUG)
                .inClass(clazz)
                .inMethod(method)
                .build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

    private void assertStaticMethodBugExactly(String clazz, String method) {
        final BugInstanceMatcher bugInstance = new BugInstanceMatcherBuilder()
                .bugType(STATIC_METHOD_BUG)
                .inClass(clazz)
                .inMethod(method)
                .build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

    private void assertObjectBugExactly(String clazz, String method, String field) {
        final BugInstanceMatcher bugInstance = new BugInstanceMatcherBuilder()
                .bugType(OBJECT_BUG)
                .inClass(clazz)
                .inMethod(method)
                .atField(field)
                .build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

    private void assertAccessibleObjectBugExactly(String clazz, String method, String field) {
        final BugInstanceMatcher bugInstance = new BugInstanceMatcherBuilder()
                .bugType(ACCESSIBLE_OBJECT_BUG)
                .inClass(clazz)
                .inMethod(method)
                .atField(field)
                .build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

    private void assertInheritableObjectBugExactly(String clazz, String method, String field) {
        final BugInstanceMatcher bugInstance = new BugInstanceMatcherBuilder()
                .bugType(INHERITABLE_OBJECT_BUG)
                .inClass(clazz)
                .inMethod(method)
                .atField(field)
                .build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

    private void assertExposedObjectBugExactly(String clazz, String method, String field) {
        final BugInstanceMatcher bugInstance = new BugInstanceMatcherBuilder()
                .bugType(EXPOSED_LOCK_OBJECT_BUG)
                .inClass(clazz)
                .inMethod(method)
                .atField(field)
                .build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

}
