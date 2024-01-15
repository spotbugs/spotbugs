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
    private static final String OBJECT_BUG = "PFL_BAD_OBJECT_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    private static final String ACCESSIBLE_OBJECT_BUG = "PFL_BAD_ACCESSIBLE_OBJECT_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";

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

    @Test
    void testBadSynchronizationWithAccessiblePrivateLockObject() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithAccessiblePrivateLock.class");

        assertNumOfBugs(1, ACCESSIBLE_OBJECT_BUG);

        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithAccessiblePrivateLock", "doStuff", "baseLock");
    }

    @Test
    void testBadSynchronizationWithAccessibleStaticLockObject2() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithAccessibleStaticLock2.class",
                "privateFinalLocks/BadSynchronizationWithAccessibleStaticLockFromParent2.class");

        assertNumOfBugs(2, OBJECT_BUG);

        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithAccessibleStaticLock2", "doStuff", "baseLock");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithAccessibleStaticLockFromParent2", "doStuff2", "lock");
    }

    @Test
    void testBadSynchronizationWithAccessibleStaticLockObject3() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithAccessibleStaticLock3.class",
                "privateFinalLocks/BadSynchronizationWithAccessibleStaticLockFromParent3.class");

        assertNumOfBugs(2, OBJECT_BUG);

        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithAccessibleStaticLock3", "doStuff", "baseLock");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithAccessibleStaticLockFromParent3", "doStuff2", "lock");
    }

    @Test
    void testBadSynchronizationWithAccessibleStaticLockObject4() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithAccessibleStaticLock4.class");

        assertNumOfBugs(1, ACCESSIBLE_OBJECT_BUG);

        /*
            @note: exposed or accessed through method, should be another bug
         */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithAccessibleStaticLock4", "doStuff", "baseLock");
    }

    @Test
    void testBadSynchronizationWithVolatileLockObject1() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithVolatileLock1.class",
                "privateFinalLocks/BadSynchronizationWithVolatileLockFromParent1.class");

        assertNumOfBugs(2, ACCESSIBLE_OBJECT_BUG);

        /*
            @note: exposed or accessed through method, should be another bug
         */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithVolatileLock1", "doSomeStuff", "baseLock"); // accessor: updateBaseLock
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithVolatileLockFromParent1", "doSomeStuff2", "lock"); // accessor: updateLock
    }

    @Test
    void testBadSynchronizationWithVolatileLockObject2() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithVolatileLock2.class",
                "privateFinalLocks/BadSynchronizationWithVolatileLockFromParent2.class");

        assertNumOfBugs(2, ACCESSIBLE_OBJECT_BUG);
        /*
            @note: exposed or accessed through method, should be another bug
         */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithVolatileLock2", "doSomeStuff", "baseLock"); // accessor: updateBaseLockWithParam
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithVolatileLockFromParent2", "doSomeStuff2", "lock"); // accessor: updateLockWithParam
    }

    @Test
    void testBadSynchronizationWithVolatileLockObject3() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithVolatileLock3.class",
                "privateFinalLocks/BadSynchronizationWithVolatileLockFromParent3.class");

        assertNumOfBugs(2, ACCESSIBLE_OBJECT_BUG);
        /*
            @note: exposed or accessed through method, should be another bug
         */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithVolatileLock3", "doSomeStuff", "baseLock"); // accessor: updateBaseLockWithLocalVariable
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithVolatileLockFromParent3", "doSomeStuff2", "lock"); // accessor: updateLockWithLocalVariable
    }

    @Test
    void testBadSynchronizationWithVolatileLockObject4() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithVolatileLock4.class",
                "privateFinalLocks/BadSynchronizationWithVolatileLockFromParent4.class");

        assertNumOfBugs(2, ACCESSIBLE_OBJECT_BUG);
        /*
            @note: exposed or accessed through method, should be another bug
         */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithVolatileLock4", "doSomeStuff", "baseLock"); // exposed by: getBaseLock
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithVolatileLockFromParent4", "doSomeStuff2", "lock"); // exposed by: getLock
    }

    @Test
    void testBadSynchronizationWithVolatileLockObject5() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithVolatileLock5.class",
                "privateFinalLocks/BadSynchronizationWithVolatileLockFromParent5.class");

        assertNumOfBugs(1, ACCESSIBLE_OBJECT_BUG);
        /*
            @note: exposed or accessed through method, should be another bug
         */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithVolatileLockFromParent5", "doSomeStuff", "baseLock"); // accessor: updateBaseLock
    }

    @Test
    void testBadSynchronizationWithVolatileLockObject6() {
        // @note: this is tough one
        performAnalysis("privateFinalLocks/BadSynchronizationWithVolatileLock6.class",
                "privateFinalLocks/BadSynchronizationWithVolatileLockFromParent6.class");

        assertNumOfBugs(1, ACCESSIBLE_OBJECT_BUG);
        /*
            @note: exposed or accessed through method, should be another bug
         */
        assertAccessibleObjectBugExactly("privateFinalLocks.BadSynchronizationWithVolatileLockFromParent6", "doSomeStuff", "baseLock"); // accessor: updateBaseLock
    }

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
        assertNumOfBugs(0, OBJECT_BUG);
        assertNumOfBugs(0, ACCESSIBLE_OBJECT_BUG);
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

}
