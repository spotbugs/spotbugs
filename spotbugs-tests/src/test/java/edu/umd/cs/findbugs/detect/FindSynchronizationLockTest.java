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
    private static final String OBJECT_BUG = "PFL_BAD_METHOD_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
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


    @Test
    void testBadMethodSynchronizationLock() {
        performAnalysis("privateFinalLocks/ClassExposingItSelf.class",
                "privateFinalLocks/ClassExposingACollectionOfItself.class",
                "privateFinalLocks/ClassExposingAMapOfItself.class");

        assertNumOfBugs(4, METHOD_BUG);

        assertMethodBugExactly("privateFinalLocks.ClassExposingItSelf", "doStuff");
        assertMethodBugExactly("privateFinalLocks.ClassExposingItSelf", "changeValue");
        assertMethodBugExactly("privateFinalLocks.ClassExposingACollectionOfItself", "doStuff");
        assertMethodBugExactly("privateFinalLocks.ClassExposingAMapOfItself", "doStuff");
    }

    @Test
    void testBadMethodSynchronizationWithPublicStaticLockObject() {
        performAnalysis("privateFinalLocks/BadMethodSynchronizationWithPublicStaticLock.class");

        assertNumOfBugs(1, METHOD_BUG);

        assertMethodBugExactly("privateFinalLocks.BadMethodSynchronizationWithPublicStaticLock", "changeValue");
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

        assertExposingObjectBugExactly("privateFinalLocks.BadSynchronizationWithExposingLockInDescendant", "doStuff1" /* in base */, "lock1"); /* Exposed by getLock1 */
        assertExposingObjectBugExactly("privateFinalLocks.BadSynchronizationWithExposingLockInDescendant", "doStuff2" /* in base */, "lock2"); /* Exposed by getLock2 */
        assertExposingObjectBugExactly("privateFinalLocks.BadSynchronizationWithExposingLockInDescendant", "doStuff3" /* in base */, "lock3"); /* Exposed by updateLock3 */
        assertExposingObjectBugExactly("privateFinalLocks.BadSynchronizationWithExposingLockInDescendant", "doStuff4" /* in base */, "lock4"); /* Exposed by updateLock4 */
        assertExposingObjectBugExactly("privateFinalLocks.BadSynchronizationWithExposingLockInDescendant", "doStuff5" /* in base */, "lock5"); /* Exposed by updateLock5 */

        // More complex cases involving longer chains of inheritance
        assertExposingObjectBugExactly("privateFinalLocks.BadSynchronizationWithExposingLockInDescendant2", "doStuff6" /* in parent */, "lock6"); /* Exposed by getLock6 */
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
    void testGoodSynchronizationWithProtectedLockInFinalClass() {
        performAnalysis("privateFinalLocks/GoodSynchronizationWithProtectedLockInFinalClass.class");

        assertNoBadLockBugs();
    }

    @Test
    void testBadSynchronizationWithExposedLockInDeclaringClass() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithLockExposedInDeclaringClass.class");

        assertNumOfBugs(9, ACCESSIBLE_OBJECT_BUG);

        /* Protected lock bugs */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithLockExposedInDeclaringClass", "doStuff1", "lock1"); /* Exposed by getLock1 */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithLockExposedInDeclaringClass", "doStuff2", "lock2"); /* Exposed by getLock2 */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithLockExposedInDeclaringClass", "doStuff3", "lock3"); /* Exposed by updateLock3 */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithLockExposedInDeclaringClass", "doStuff4", "lock4"); /* Exposed by updateLock4 */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithLockExposedInDeclaringClass", "doStuff5", "lock5"); /* Exposed by updateLock5 */

        /* Private lock bugs */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithLockExposedInDeclaringClass", "doStuff6", "lock6"); /* Exposed by getLock6 */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithLockExposedInDeclaringClass", "doStuff7", "lock7"); /* Exposed by updateLock7 */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithLockExposedInDeclaringClass", "doStuff8", "lock8"); /* Exposed by updateLock8 */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithLockExposedInDeclaringClass", "doStuff9", "lock9"); /* Exposed by updateLock9 */
    }

    @Test
    void testBadSynchronizationWithExposedLockInHierarchy() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithExposedLockFromHierarchy.class",
                "privateFinalLocks/BadSynchronizationUsingExposedLockFromHierarchy.class",
                "privateFinalLocks/BadSynchronizationUsingExposedLockFromHierarchy2.class");


        assertNumOfBugs(6, ACCESSIBLE_OBJECT_BUG);

        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationUsingExposedLockFromHierarchy", "doStuff1", "lock1"); /* Exposed by getLock1 in parent */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationUsingExposedLockFromHierarchy", "doStuff2", "lock2"); /* Exposed by getLock2 in parent */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationUsingExposedLockFromHierarchy", "doStuff3", "lock3"); /* Exposed by updateLock3 in parent */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationUsingExposedLockFromHierarchy", "doStuff4", "lock4"); /* Exposed by updateLock4 in parent */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationUsingExposedLockFromHierarchy", "doStuff5", "lock5"); /* Exposed by updateLock5 in parent */

        // More complex cases involving longer chains of inheritance
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationUsingExposedLockFromHierarchy2", "doStuff6", "lock6"); /* Exposed by getLock6 in parent */
    }

    @Test
    void testBadSynchronizationByExposingLockFromHierarchy() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithExposingLockFromHierarchy.class",
                "privateFinalLocks/BadSynchronizationByExposingLockFromHierarchy.class");

        assertNumOfBugs(5, ACCESSIBLE_OBJECT_BUG);

        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationByExposingLockFromHierarchy", "doStuff1", "lock1"); /* Exposed by getLock1 */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationByExposingLockFromHierarchy", "doStuff2", "lock2"); /* Exposed by getLock1 */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationByExposingLockFromHierarchy", "doStuff3", "lock3"); /* Exposed by updateLock3 */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationByExposingLockFromHierarchy", "doStuff4", "lock4"); /* Exposed by updateLock4 */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationByExposingLockFromHierarchy", "doStuff5", "lock5"); /* Exposed by updateLock5 */
    }


    /**
     * The following tests are work in progress
 *                  OBJECT_BUG
     */



    /**
     * End of work in progress
     */



    @Test
    void testBadSynchronizationLockAcquiredFromParent() {
        performAnalysis("privateFinalLocks/BadSynchronizationLockBase.class",
                "privateFinalLocks/BadSynchronizationWithLocksFromBase.class",
                "privateFinalLocks/BadSynchronizationLockBaseWithMultipleMethods.class");

        assertNumOfBugs(5, OBJECT_BUG);

        assertObjectBugExactly("privateFinalLocks.BadSynchronizationLockBase", "doStuff", "baseLock");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithLocksFromBase", "doOtherStuff", "baseLock");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithLocksFromBase", "changeValue", "lock");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationLockBaseWithMultipleMethods", "doStuff", "baseLock");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationLockBaseWithMultipleMethods", "doOtherStuff", "baseLock");
    }

    @Test
    void testBadSynchronizationWithAccessibleFinalLockObject() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithPublicFinalLock.class",
                "privateFinalLocks/BadSynchronizationWithPublicFinalLockFromParent.class");

        assertNumOfBugs(3, OBJECT_BUG);

        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithPublicFinalLock", "doSomeStuff", "baseLock");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithPublicFinalLockFromParent", "doSomeStuff2", "baseLock");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithPublicFinalLockFromParent", "doSomeStuff3", "lock");
    }

    @Test
    void testBadSynchronizationWithAccessibleStaticLockObject1() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithAccessibleStaticLock1.class",
                "privateFinalLocks/BadSynchronizationWithAccessibleStaticLockFromParent1.class");

        assertNumOfBugs(2, OBJECT_BUG);

        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithAccessibleStaticLock1", "doStuff", "baseLock");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithAccessibleStaticLockFromParent1", "doStuff2", "lock");
    }


//    @Test
//    void testBadSynchronizationWithVolatileLockObject6() {
//
//        performAnalysis("privateFinalLocks/BadSynchronizationWithExposedLockToUntrustedCode.class",
//                "privateFinalLocks/BadSynchronizationWithLockFromParent.class");
//
////        assertNumOfBugs(1, INHERITED_OBJECT_BUG);
////
////        assertExposedLocksThroughInheritance("privateFinalLocks.BadSynchronizationWithExposedLockToUntrustedCode", "doSomeStuff", "baseLock"); // accessor in descendant
////
//        assertNumOfBugs(2, OBJECT_BUG);
//
//        //INHERITED_OBJECT_BUG
//        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithExposedLockToUntrustedCode", "doSomeStuff", "baseLock"); // accessor in descendant
//        //OBJECT_BUG
//        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithLockFromParent", "doSomeStuff2", "lock"); // accessor in descendant
//    }

    @Test
    void testBadSynchronizationWithPublicNonFinalLockObject() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithPublicNonFinalLock.class",
                "privateFinalLocks/BadSynchronizationWithNonFinalLockFromParent.class");

        assertNumOfBugs(3, OBJECT_BUG);

        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithPublicNonFinalLock", "doSomeStuff", "baseLock");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithNonFinalLockFromParent", "doSomeStuff2", "baseLock");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithNonFinalLockFromParent", "doSomeStuff3", "lock");
    }



    @Test
    void testGoodSynchronizationWithPrivateFinalLockObject() {
        performAnalysis("privateFinalLocks/GoodSynchronizationWithPrivateFinalLock.class");

        assertNoBadLockBugs();
    }

    @Test
    void testGoodSynchronizationWithPrivateStaticLockObject() {
        performAnalysis("privateFinalLocks/GoodSynchronizationWithPrivateStaticLock.class");

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
