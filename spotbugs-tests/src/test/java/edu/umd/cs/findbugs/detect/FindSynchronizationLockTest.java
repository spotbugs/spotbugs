package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class FindSynchronizationLockTest extends AbstractIntegrationTest {

    /**
     * @todo Which of these should have priority in case of overlapping bugs? Or should we report both of them?
     */
    /**
     * Report:<br>
     *      0) the class where the bug is detected <br>
     *      1) the method used for synchronization<br>
     *      2) the exposing methods<br>
     */
    private static final String METHOD_BUG = "PFL_BAD_METHOD_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    /**
     * Report:<br>
     *      0) the class where the bug is detected <br>
     *      1) the static method used for synchronization<br>
     */
    private static final String STATIC_METHOD_BUG = "PFL_BAD_STATIC_METHOD_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    /**
     * Report:<br>
     *      0) the class where the bug is detected <br>
     *      1) the method where it was used as lock (here)<br>
     *      2) the lock object itself<br>
     */
    private static final String OBJECT_BUG = "PFL_BAD_OBJECT_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    /**
     * Report:<br>
     *      @note The class reported should be the class where the lock was declared or where the lock was used?<br>
     *      0) the class where the bug is detected <br>
     *      1) the method where it was used as lock (here)<br>
     *      2) the lock object itself<br>
     *      3) accessor methods(can be multiple places)<br>
     */
    private static final String ACCESSIBLE_OBJECT_BUG = "PFL_BAD_ACCESSIBLE_OBJECT_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    /**
     * Report:<br>
     *      0) the class where it was detected (here)<br>
     *      1) the method where it was used as lock (here)<br>
     *      2) the lock object itself (here)<br>
     */
    private static final String INHERITED_OBJECT_BUG = "PFL_BAD_INHERITED_OBJECT_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    /**
     * Report:<br>
     *      0) the class where it was detected<br>
     *      1) the method where it was used as lock (in parent)<br>
     *      2) the lock object itself<br>
     *      3) the methods where it was exposed (in descendant)<br>
    */
    private static final String EXPOSING_LOCK_OBJECT_BUG = "PFL_BAD_EXPOSING_OBJECT_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";


    /**
     * The following tests are for object synchronization bugs
     */

    @Test
    void testBadMethodSynchronizationLock() {
        performAnalysis("privateFinalLocks/BadMethodSynchronizationWithClassExposingItSelf.class",
                "privateFinalLocks/BadMethodSynchronizationWithClassExposingACollectionOfItself.class",
                "privateFinalLocks/BadMethodSynchronizationWithClassExposingAMapOfItself.class");

        assertNumOfBugs(4, METHOD_BUG);

        assertMethodBugExactly("privateFinalLocks.BadMethodSynchronizationWithClassExposingItSelf", "doStuff");
        assertMethodBugExactly("privateFinalLocks.BadMethodSynchronizationWithClassExposingItSelf", "doStuff2");
        assertMethodBugExactly("privateFinalLocks.BadMethodSynchronizationWithClassExposingACollectionOfItself", "doStuff");
        assertMethodBugExactly("privateFinalLocks.BadMethodSynchronizationWithClassExposingAMapOfItself", "doStuff");
    }

    @Test
    void testBadMethodSynchronizationWithPublicStaticLockObject() {
        performAnalysis("privateFinalLocks/BadMethodSynchronizationWithPublicStaticSynchronization.class");

        assertNumOfBugs(1, STATIC_METHOD_BUG);

        assertStaticMethodBugExactly("privateFinalLocks.BadMethodSynchronizationWithPublicStaticSynchronization", "doStuff");
    }

    @Test

    void testBadMethodSynchronizationWithMixedBugs() {
        performAnalysis("privateFinalLocks/BadMixedMethodSynchronization.class");

        /**
         * @todo Which one? METHOD_BUG or STATIC_METHOD_BUG? Or both?
         */
        assertNumOfBugs(1, METHOD_BUG);
        assertNumOfBugs(1, STATIC_METHOD_BUG);

        assertMethodBugExactly("privateFinalLocks.BadMethodSynchronizationWithClassExposingItSelf", "doStuff");
        assertStaticMethodBugExactly("privateFinalLocks.BadMethodSynchronizationWithPublicStaticSynchronization", "doStuff");
    }

    @Test
    void goodMethodSynchronizationLock() {
        performAnalysis("privateFinalLocks/GoodMethodSynchronization.class");

        assertNoBadLockBugs();
    }

    /**
     * The following tests are for object synchronization bugs
     */

    @Test
    void testBadSynchronizationExposedLockInDescendant() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithExposedLockInDescendant.class",
                "privateFinalLocks/BadSynchronizationWithExposingLockInDescendant.class",
                "privateFinalLocks/BadSynchronizationWithExposingLockInDescendant2.class");

        assertNumOfBugs(6, EXPOSING_LOCK_OBJECT_BUG);

        assertExposingObjectBugExactly("privateFinalLocks.BadSynchronizationWithExposingLockInDescendant", "doStuff1" /* in base */,
                "lock1"); /* Exposed by getLock1 */
        assertExposingObjectBugExactly("privateFinalLocks.BadSynchronizationWithExposingLockInDescendant", "doStuff2" /* in base */,
                "lock2"); /* Exposed by getLock2 */
        assertExposingObjectBugExactly("privateFinalLocks.BadSynchronizationWithExposingLockInDescendant", "doStuff3" /* in base */,
                "lock3"); /* Exposed by updateLock3 */
        assertExposingObjectBugExactly("privateFinalLocks.BadSynchronizationWithExposingLockInDescendant", "doStuff4" /* in base */,
                "lock4"); /* Exposed by updateLock4 */
        assertExposingObjectBugExactly("privateFinalLocks.BadSynchronizationWithExposingLockInDescendant", "doStuff5" /* in base */,
                "lock5"); /* Exposed by updateLock5 */

        // More complex cases involving longer chains of inheritance
        assertExposingObjectBugExactly("privateFinalLocks.BadSynchronizationWithExposingLockInDescendant2", "doStuff6" /* in parent */,
                "lock6"); /* Exposed by getLock6 */
    }

    @Test
    void testBadSynchronizationWithInheritedLocks() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithPotentiallyInheritedProtectedLocks.class",
                "privateFinalLocks/BadSynchronizationWithExposedPackagePrivateLocks.class");

        assertNumOfBugs(8, INHERITED_OBJECT_BUG);

        /* Protected bugs */
        assertInheritedObjectBugExactly("privateFinalLocks.BadSynchronizationWithPotentiallyInheritedProtectedLocks", "doStuff1", "lock1");
        assertInheritedObjectBugExactly("privateFinalLocks.BadSynchronizationWithPotentiallyInheritedProtectedLocks", "doStuff2", "lock2");
        assertInheritedObjectBugExactly("privateFinalLocks.BadSynchronizationWithPotentiallyInheritedProtectedLocks", "doStuff3", "lock3");
        assertInheritedObjectBugExactly("privateFinalLocks.BadSynchronizationWithPotentiallyInheritedProtectedLocks", "doStuff4", "lock4");

        /* Package private bugs */
        assertInheritedObjectBugExactly("privateFinalLocks.BadSynchronizationWithExposedPackagePrivateLocks", "doStuff1", "lock1");
        assertInheritedObjectBugExactly("privateFinalLocks.BadSynchronizationWithExposedPackagePrivateLocks", "doStuff2", "lock2");
        assertInheritedObjectBugExactly("privateFinalLocks.BadSynchronizationWithExposedPackagePrivateLocks", "doStuff3", "lock3");
        assertInheritedObjectBugExactly("privateFinalLocks.BadSynchronizationWithExposedPackagePrivateLocks", "doStuff4", "lock4");
    }

    @Test
    void testBadSynchronizationWithExposedLockInDeclaringClass() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithLockExposedInDeclaringClass.class");

        assertNumOfBugs(9, ACCESSIBLE_OBJECT_BUG);

        /* Protected lock bugs */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithLockExposedInDeclaringClass", "doStuff1",
                "lock1"); /* Exposed by getLock1 */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithLockExposedInDeclaringClass", "doStuff2",
                "lock2"); /* Exposed by getLock2 */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithLockExposedInDeclaringClass", "doStuff3",
                "lock3"); /* Exposed by updateLock3 */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithLockExposedInDeclaringClass", "doStuff4",
                "lock4"); /* Exposed by updateLock4 */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithLockExposedInDeclaringClass", "doStuff5",
                "lock5"); /* Exposed by updateLock5 */

        /* Private lock bugs */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithLockExposedInDeclaringClass", "doStuff6",
                "lock6"); /* Exposed by getLock6 */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithLockExposedInDeclaringClass", "doStuff7",
                "lock7"); /* Exposed by updateLock7 */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithLockExposedInDeclaringClass", "doStuff8",
                "lock8"); /* Exposed by updateLock8 */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithLockExposedInDeclaringClass", "doStuff9",
                "lock9"); /* Exposed by updateLock9 */
    }

    @Test
    void testBadSynchronizationWithExposedLockInHierarchy() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithExposedLockFromHierarchy.class",
                "privateFinalLocks/BadSynchronizationUsingExposedLockFromHierarchy.class",
                "privateFinalLocks/BadSynchronizationUsingExposedLockFromHierarchy2.class");


        assertNumOfBugs(6, ACCESSIBLE_OBJECT_BUG);

        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationUsingExposedLockFromHierarchy", "doStuff1",
                "lock1"); /* Exposed by getLock1 in parent */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationUsingExposedLockFromHierarchy", "doStuff2",
                "lock2"); /* Exposed by getLock2 in parent */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationUsingExposedLockFromHierarchy", "doStuff3",
                "lock3"); /* Exposed by updateLock3 in parent */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationUsingExposedLockFromHierarchy", "doStuff4",
                "lock4"); /* Exposed by updateLock4 in parent */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationUsingExposedLockFromHierarchy", "doStuff5",
                "lock5"); /* Exposed by updateLock5 in parent */

        // More complex cases involving longer chains of inheritance
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationUsingExposedLockFromHierarchy2", "doStuff6",
                "lock6"); /* Exposed by getLock6 in parent */
    }

    @Test
    void testBadSynchronizationByExposingLockFromHierarchy() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithExposingLockFromHierarchy.class",
                "privateFinalLocks/BadSynchronizationByExposingLockFromHierarchy.class");

        assertNumOfBugs(5, ACCESSIBLE_OBJECT_BUG);

        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationByExposingLockFromHierarchy", "doStuff1",
                "lock1"); /* Exposed by getLock1 */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationByExposingLockFromHierarchy", "doStuff2",
                "lock2"); /* Exposed by getLock1 */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationByExposingLockFromHierarchy", "doStuff3",
                "lock3"); /* Exposed by updateLock3 */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationByExposingLockFromHierarchy", "doStuff4",
                "lock4"); /* Exposed by updateLock4 */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationByExposingLockFromHierarchy", "doStuff5",
                "lock5"); /* Exposed by updateLock5 */
    }

    @Test
    void testBadSynchronizationWithPublicLocksInPlace() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithPublicLocksInPlace.class");

        assertNumOfBugs(6, OBJECT_BUG);

        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithPublicLocksInPlace", "doStuff1", "lock1");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithPublicLocksInPlace", "doStuff1Again", "lock1");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithPublicLocksInPlace", "doStuff2", "lock2");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithPublicLocksInPlace", "doStuff3", "lock3");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithPublicLocksInPlace", "doStuff4", "lock4");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithPublicLocksInPlace", "doStuff5", "lock5");
    }

    @Test
    void testBadSynchronizationWithVisibleLocksExposedWithPolymorphism() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithVisibleLocksExposedWithPolymorphism.class",
                "privateFinalLocks/BadSynchronizationWithVisibleLocksFromHierarchy.class");

        assertNumOfBugs(14, OBJECT_BUG);

        // Public locks - Do we want to report them in descendant if we have already reported in parent? Do we want to try optimizing it?
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff1", "lock1");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff1Again", "lock1");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff2", "lock2");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff3", "lock3");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff4", "lock4");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff5", "lock5");

        // Protected locks - it is important that the declaring class is NOT final
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff6", "lock6");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff7", "lock7");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff8", "lock8");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff9", "lock9");

        // Package private locks
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff10", "lock10");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff11", "lock11");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff12", "lock12");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithVisibleLocksFromHierarchy", "doStuff13", "lock13");
    }


    /**
     * The following tests are work in progress
    *                  OBJECT_BUG
     */



    /**
     * End of work in progress
     */

    @Test
    void testGoodSynchronizationWithPrivateFinalLockObject() {
        performAnalysis("privateFinalLocks/GoodSynchronizationWithPrivateFinalLock.class");

        assertNoBadLockBugs();
    }

    @Test
    void testGoodSynchronizationWithProtectedLockInFinalClass() {
        performAnalysis("privateFinalLocks/GoodSynchronizationWithProtectedLockInFinalClass.class");

        assertNoBadLockBugs();
    }

    @Test
    void testGoodSynchronizationWithInheritance() {
        performAnalysis("privateFinalLocks/GoodSynchronizationWithInheritance.class",
                "privateFinalLocks/GoodSynchronizationInheritedFromParent.class");

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
