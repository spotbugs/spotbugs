package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class FindSynchronizationLockTest extends AbstractIntegrationTest {

    private static final String METHOD_BUG = "PFL_BAD_METHOD_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    private static final String STATIC_METHOD_BUG = "PFL_BAD_STATIC_METHOD_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    private static final String OBJECT_BUG = "PFL_BAD_OBJECT_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    private static final String ACCESSIBLE_OBJECT_BUG = "PFL_BAD_ACCESSIBLE_OBJECT_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    private static final String INHERITED_OBJECT_BUG = "PFL_BAD_INHERITED_OBJECT_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    private static final String EXPOSING_LOCK_OBJECT_BUG = "PFL_BAD_EXPOSING_OBJECT_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";

    /**
     * The following tests are for object synchronization bugs
     */
    @Test
    void testBadMethodSynchronizationLock() {
        performAnalysis("synchronizationLocks/privateFinalLocks/BadMethodSynchronizationWithClassExposingItSelf.class",
                "synchronizationLocks/privateFinalLocks/BadMethodSynchronizationWithClassExposingACollectionOfItself.class",
                "synchronizationLocks/privateFinalLocks/BadMethodSynchronizationWithClassExposingAMapOfItself.class",
                "synchronizationLocks/privateFinalLocks/BadMethodSynchronizationWithClassExposingMixedGenericOfItself.class");

        assertNumOfBugs(5, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITED_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSING_LOCK_OBJECT_BUG);

        assertMethodBugExactly("synchronizationLocks.privateFinalLocks.BadMethodSynchronizationWithClassExposingItSelf", "doStuff");
        assertMethodBugExactly("synchronizationLocks.privateFinalLocks.BadMethodSynchronizationWithClassExposingItSelf", "doStuff2");
        assertMethodBugExactly("synchronizationLocks.privateFinalLocks.BadMethodSynchronizationWithClassExposingACollectionOfItself", "doStuff");
        assertMethodBugExactly("synchronizationLocks.privateFinalLocks.BadMethodSynchronizationWithClassExposingAMapOfItself", "doStuff");
        assertMethodBugExactly("synchronizationLocks.privateFinalLocks.BadMethodSynchronizationWithClassExposingMixedGenericOfItself", "doStuff");
    }

    @Test
    void testBadMethodSynchronizationWithPublicStaticLockObject() {
        performAnalysis("synchronizationLocks/privateFinalLocks/BadMethodSynchronizationWithPublicStaticSynchronization.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(1, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITED_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSING_LOCK_OBJECT_BUG);

        assertStaticMethodBugExactly("synchronizationLocks.privateFinalLocks.BadMethodSynchronizationWithPublicStaticSynchronization", "doStuff");
    }

    @Test
    void testBadMethodSynchronizationWithMixedBugs() {
        performAnalysis("synchronizationLocks/privateFinalLocks/BadMixedMethodSynchronization.class");

        assertNumOfBugs(1, METHOD_BUG);
        assertNumOfBugs(1, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITED_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSING_LOCK_OBJECT_BUG);

        assertMethodBugExactly("synchronizationLocks.privateFinalLocks.BadMixedMethodSynchronization", "doStuff");
        assertStaticMethodBugExactly("synchronizationLocks.privateFinalLocks.BadMixedMethodSynchronization", "doStuff");
    }

    @Test
    void goodMethodSynchronizationLock() {
        performAnalysis("synchronizationLocks/privateFinalLocks/GoodMethodSynchronization.class");

        assertNoBadLockBugs();
    }

    /**
     * The following tests are for object synchronization bugs
     */
    @Test
    void testBadSynchronizationExposedLockInDescendant() {
        performAnalysis("synchronizationLocks/privateFinalLocks/BadSynchronizationWithExposedLockInDescendant.class",
                "synchronizationLocks/privateFinalLocks/BadSynchronizationWithExposingLockInDescendant.class",
                "synchronizationLocks/privateFinalLocks/BadSynchronizationWithExposingLockInDescendant2.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(1, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(5, INHERITED_OBJECT_BUG);
        assertNumOfBugs(6, EXPOSING_LOCK_OBJECT_BUG);

        assertExposingObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithExposingLockInDescendant",
                "doStuff1" /* in base */,
                "lock1"); /* Exposed by getLock1 */
        assertInheritedObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithExposedLockInDescendant", "doStuff1", "lock1");

        assertExposingObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithExposingLockInDescendant",
                "doStuff2" /* in base */,
                "lock2"); /* Exposed by getLock2 */
        assertInheritedObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithExposedLockInDescendant", "doStuff2", "lock2");

        assertExposingObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithExposingLockInDescendant",
                "doStuff3" /* in base */,
                "lock3"); /* Exposed by updateLock3 */
        assertInheritedObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithExposedLockInDescendant", "doStuff3", "lock3");

        assertExposingObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithExposingLockInDescendant",
                "doStuff4" /* in base */,
                "lock4"); /* Exposed by updateLock4 */
        assertInheritedObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithExposedLockInDescendant", "doStuff4", "lock4");

        assertExposingObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithExposingLockInDescendant",
                "doStuff5" /* in base */,
                "lock5"); /* Exposed by updateLock5 */
        assertInheritedObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithExposedLockInDescendant", "doStuff5", "lock5");

        // More complex cases involving longer chains of inheritance
        assertExposingObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithExposingLockInDescendant2",
                "doStuff6" /* in parent */,
                "lock6"); /* Exposed by getLock6 */
        assertObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithExposingLockInDescendant", "doStuff6", "lock6");
    }

    @Test
    void testBadSynchronizationWithInheritedLocks() {
        performAnalysis("synchronizationLocks/privateFinalLocks/BadSynchronizationWithPotentiallyInheritedProtectedLocks.class",
                "synchronizationLocks/privateFinalLocks/BadSynchronizationWithExposedPackagePrivateLocks.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(8, INHERITED_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSING_LOCK_OBJECT_BUG);

        /* Protected bugs */
        assertInheritedObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithPotentiallyInheritedProtectedLocks", "doStuff1",
                "lock1");
        assertInheritedObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithPotentiallyInheritedProtectedLocks", "doStuff2",
                "lock2");
        assertInheritedObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithPotentiallyInheritedProtectedLocks", "doStuff3",
                "lock3");
        assertInheritedObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithPotentiallyInheritedProtectedLocks", "doStuff4",
                "lock4");

        /* Package private bugs */
        assertInheritedObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithExposedPackagePrivateLocks", "doStuff1",
                "lock1");
        assertInheritedObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithExposedPackagePrivateLocks", "doStuff2",
                "lock2");
        assertInheritedObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithExposedPackagePrivateLocks", "doStuff3",
                "lock3");
        assertInheritedObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithExposedPackagePrivateLocks", "doStuff4",
                "lock4");
    }

    @Test
    void testBadSynchronizationWithExposedLockInDeclaringClass() {
        performAnalysis("synchronizationLocks/privateFinalLocks/BadSynchronizationWithLockExposedInDeclaringClass.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(9, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITED_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSING_LOCK_OBJECT_BUG);

        /* Protected lock bugs */
        assertAccessibleObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithLockExposedInDeclaringClass", "doStuff1",
                "lock1"); /* Exposed by getLock1 */
        assertAccessibleObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithLockExposedInDeclaringClass", "doStuff2",
                "lock2"); /* Exposed by getLock2 */
        assertAccessibleObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithLockExposedInDeclaringClass", "doStuff3",
                "lock3"); /* Exposed by updateLock3 */
        assertAccessibleObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithLockExposedInDeclaringClass", "doStuff4",
                "lock4"); /* Exposed by updateLock4 */
        assertAccessibleObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithLockExposedInDeclaringClass", "doStuff5",
                "lock5"); /* Exposed by updateLock5 */

        /* Private lock bugs */
        assertAccessibleObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithLockExposedInDeclaringClass", "doStuff6",
                "lock6"); /* Exposed by getLock6 */
        assertAccessibleObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithLockExposedInDeclaringClass", "doStuff7",
                "lock7"); /* Exposed by updateLock7 */
        assertAccessibleObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithLockExposedInDeclaringClass", "doStuff8",
                "lock8"); /* Exposed by updateLock8 */
        assertAccessibleObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithLockExposedInDeclaringClass", "doStuff9",
                "lock9"); /* Exposed by updateLock9 */
    }

    @Test
    void testBadSynchronizationWithExposedLockInHierarchy() {
        performAnalysis("synchronizationLocks/privateFinalLocks/BadSynchronizationWithExposedLockFromHierarchy.class",
                "synchronizationLocks/privateFinalLocks/BadSynchronizationUsingExposedLockFromHierarchy.class",
                "synchronizationLocks/privateFinalLocks/BadSynchronizationUsingExposedLockFromHierarchy2.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(6, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITED_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSING_LOCK_OBJECT_BUG);

        assertAccessibleObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationUsingExposedLockFromHierarchy", "doStuff1",
                "lock1"); /* Exposed by getLock1 in parent */
        assertAccessibleObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationUsingExposedLockFromHierarchy", "doStuff2",
                "lock2"); /* Exposed by getLock2 in parent */
        assertAccessibleObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationUsingExposedLockFromHierarchy", "doStuff3",
                "lock3"); /* Exposed by updateLock3 in parent */
        assertAccessibleObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationUsingExposedLockFromHierarchy", "doStuff4",
                "lock4"); /* Exposed by updateLock4 in parent */
        assertAccessibleObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationUsingExposedLockFromHierarchy", "doStuff5",
                "lock5"); /* Exposed by updateLock5 in parent */

        // More complex cases involving longer chains of inheritance
        assertAccessibleObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationUsingExposedLockFromHierarchy2", "doStuff6",
                "lock6"); /* Exposed by getLock6 in parent */
    }

    @Test
    void testBadSynchronizationByExposingLockFromHierarchy() {
        performAnalysis("synchronizationLocks/privateFinalLocks/BadSynchronizationWithExposingLockFromHierarchy.class",
                "synchronizationLocks/privateFinalLocks/BadSynchronizationByExposingLockFromHierarchy.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(5, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITED_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSING_LOCK_OBJECT_BUG);

        assertAccessibleObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationByExposingLockFromHierarchy", "doStuff1",
                "lock1"); /* Exposed by getLock1 */
        assertAccessibleObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationByExposingLockFromHierarchy", "doStuff2",
                "lock2"); /* Exposed by getLock1 */
        assertAccessibleObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationByExposingLockFromHierarchy", "doStuff3",
                "lock3"); /* Exposed by updateLock3 */
        assertAccessibleObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationByExposingLockFromHierarchy", "doStuff4",
                "lock4"); /* Exposed by updateLock4 */
        assertAccessibleObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationByExposingLockFromHierarchy", "doStuff5",
                "lock5"); /* Exposed by updateLock5 */
    }

    @Test
    void testBadSynchronizationWithPublicLocksInPlace() {
        performAnalysis("synchronizationLocks/privateFinalLocks/BadSynchronizationWithPublicLocksInPlace.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(6, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITED_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSING_LOCK_OBJECT_BUG);

        assertObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithPublicLocksInPlace", "doStuff1", "lock1");
        assertObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithPublicLocksInPlace", "doStuff1Again", "lock1");
        assertObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithPublicLocksInPlace", "doStuff2", "lock2");
        assertObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithPublicLocksInPlace", "doStuff3", "lock3");
        assertObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithPublicLocksInPlace", "doStuff4", "lock4");
        assertObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithPublicLocksInPlace", "doStuff5", "lock5");
    }

    @Test
    void testBadSynchronizationWithVisibleLocksExposedWithPolymorphism() {
        performAnalysis("synchronizationLocks/privateFinalLocks/BadSynchronizationWithVisibleLocksExposedWithPolymorphism.class",
                "synchronizationLocks/privateFinalLocks/BadSynchronizationWithVisibleLocksFromHierarchy.class");

        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(14, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITED_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSING_LOCK_OBJECT_BUG);

        // Public locks
        assertObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff1", "lock1");
        assertObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff1Again", "lock1");
        assertObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff2", "lock2");
        assertObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff3", "lock3");
        assertObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff4", "lock4");
        assertObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff5", "lock5");

        // Protected locks - it is important that the declaring class is NOT final
        assertObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff6", "lock6");
        assertObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff7", "lock7");
        assertObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff8", "lock8");
        assertObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff9", "lock9");

        // Package private locks
        assertObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff10", "lock10");
        assertObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff11", "lock11");
        assertObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff12", "lock12");
        assertObjectBugExactly("synchronizationLocks.privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff13", "lock13");
    }

    @Test
    void testGoodSynchronizationWithPrivateFinalLockObject() {
        performAnalysis("synchronizationLocks/privateFinalLocks/GoodSynchronizationWithPrivateFinalLock.class");

        assertNoBadLockBugs();
    }

    @Test
    void testGoodSynchronizationWithProtectedLockInFinalClass() {
        performAnalysis("synchronizationLocks/privateFinalLocks/GoodSynchronizationWithProtectedLockInFinalClass.class");

        assertNoBadLockBugs();
    }

    @Test
    void testGoodSynchronizationWithInheritance() {
        performAnalysis("synchronizationLocks/privateFinalLocks/GoodSynchronizationWithInheritance.class",
                "synchronizationLocks/privateFinalLocks/GoodSynchronizationInheritedFromParent.class");

        assertNoBadLockBugs();
    }

    @Test
    void testGoodSynchronizationWithBuiltinMethodsSeemToExposeLock() {
        performAnalysis("synchronizationLocks/privateFinalLocks/GoodSynchronizationWithBuiltinMethodExposingLock.class");

        assertNoBadLockBugs();
    }


    private void assertNoBadLockBugs() {
        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, STATIC_METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
        assertNumOfBugs(0, INHERITED_OBJECT_BUG);
        assertNumOfBugs(0, EXPOSING_LOCK_OBJECT_BUG);
    }

    private void assertNumOfBugs(int number, String bugType) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().bugType(bugType).build();
        assertThat(getBugCollection(), containsExactly(number, bugTypeMatcher));
    }

    private void assertMethodBugExactly(String clazz, String method) {
        BugInstanceMatcherBuilder bugInstanceMatcherBuilder = new BugInstanceMatcherBuilder()
                .bugType(METHOD_BUG)
                .inClass(clazz)
                .inMethod(method);

        final BugInstanceMatcher bugInstance = bugInstanceMatcherBuilder.build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

    private void assertStaticMethodBugExactly(String clazz, String method) {
        BugInstanceMatcherBuilder bugInstanceMatcherBuilder = new BugInstanceMatcherBuilder()
                .bugType(STATIC_METHOD_BUG)
                .inClass(clazz)
                .inMethod(method);

        final BugInstanceMatcher bugInstance = bugInstanceMatcherBuilder.build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

    private void assertObjectBugExactly(String clazz, String method, String field) {
        BugInstanceMatcherBuilder bugInstanceMatcherBuilder = new BugInstanceMatcherBuilder()
                .bugType(OBJECT_BUG)
                .inClass(clazz)
                .inMethod(method)
                .atField(field);
        final BugInstanceMatcher bugInstance = bugInstanceMatcherBuilder.build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

    private void assertAccessibleObjectBugExactly(String clazz, String method, String field) {
        BugInstanceMatcherBuilder bugInstanceMatcherBuilder = new BugInstanceMatcherBuilder()
                .bugType(ACCESSIBLE_OBJECT_BUG)
                .inClass(clazz)
                .inMethod(method)
                .atField(field);
        final BugInstanceMatcher bugInstance = bugInstanceMatcherBuilder.build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

    private void assertInheritedObjectBugExactly(String clazz, String method, String field) {
        BugInstanceMatcherBuilder bugInstanceMatcherBuilder = new BugInstanceMatcherBuilder()
                .bugType(INHERITED_OBJECT_BUG)
                .inClass(clazz)
                .inMethod(method)
                .atField(field);
        final BugInstanceMatcher bugInstance = bugInstanceMatcherBuilder.build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

    private void assertExposingObjectBugExactly(String clazz, String method, String field) {
        BugInstanceMatcherBuilder bugInstanceMatcherBuilder = new BugInstanceMatcherBuilder()
                .bugType(EXPOSING_LOCK_OBJECT_BUG)
                .inClass(clazz)
                .inMethod(method)
                .atField(field);
        final BugInstanceMatcher bugInstance = bugInstanceMatcherBuilder.build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

}
