package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class FindSynchronizationLockTest extends AbstractIntegrationTest {

    private static final String METHOD_BUG = "PFL_BAD_METHOD_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    private static final String OBJECT_BUG = "PFL_BAD_OBJECT_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";

    @Test
    void testBadMethodSynchronizationLock() {
        performAnalysis("privateFinalLocks/ClassExposingItSelf.class", "privateFinalLocks/ClassExposingACollectionOfItself.class",
                "privateFinalLocks/ClassExposingAMapOfItself.class");

        assertNumOfBugs(4, METHOD_BUG);

        assertMethodBugExactly("privateFinalLocks.ClassExposingItSelf", "doStuff", Set.of("lookup", "lookup2"));
        assertMethodBugExactly("privateFinalLocks.ClassExposingItSelf", "changeValue", Set.of("lookup", "lookup2"));
        assertMethodBugExactly("privateFinalLocks.ClassExposingACollectionOfItself", "doStuff", Set.of("getCollection"));
        assertMethodBugExactly("privateFinalLocks.ClassExposingAMapOfItself", "doStuff", Set.of("getMap"));
    }

    @Test
    void testBadMethodSynchronizationWithPublicStaticLockObject() {
        performAnalysis("privateFinalLocks/BadMethodSynchronizationWithPublicStaticLock.class");

        assertNumOfBugs(1, METHOD_BUG);

        assertMethodBugExactly("privateFinalLocks.BadMethodSynchronizationWithPublicStaticLock", "changeValue", Collections.emptySet());
    }

    @Test
    void testBadSynchronizationLockAcquiredFromParent() {
        performAnalysis("privateFinalLocks/BadSynchronizationLockBase.class", "privateFinalLocks/BadSynchronizationWithLocksFromBase.class",
                "privateFinalLocks/BadSynchronizationLockBaseWithMultipleMethods.class");

        assertNumOfBugs(5, OBJECT_BUG);

        assertObjectBugExactly("privateFinalLocks.BadSynchronizationLockBase", "doStuff", "baseLock");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithLocksFromBase", "doOtherStuff", "baseLock");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithLocksFromBase", "changeValue", "lock");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationLockBaseWithMultipleMethods", "doStuff", "baseLock");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationLockBaseWithMultipleMethods", "doOtherStuff", "baseLock");
    }

    @Test
    void goodMethodSynchronizationLock() {
        performAnalysis("privateFinalLocks/GoodMethodSynchronization.class");

        assertNoBadLockBugs();
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
    void testBadSynchronizationWithPubliclyAccessibleNonFinalLockObject() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithPubliclyAccessibleNonFinalLock.class",
                "privateFinalLocks/BadSynchronizationWithPubliclyAccessibleNonFinalLockFromParent.class");

        assertNumOfBugs(2, OBJECT_BUG);

        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithPubliclyAccessibleNonFinalLock", "doSomeStuff", "baseLock");
        assertObjectBugExactly("privateFinalLocks.BadSynchronizationWithPubliclyAccessibleNonFinalLockFromParent", "doSomeStuff2", "lock");
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
    void testGoodSynchronizationWithInheritance() {
        performAnalysis("privateFinalLocks/GoodSynchronizationWithInheritance.class",
                "privateFinalLocks/GoodSynchronizationInheritedFromParent.class");

        assertNoBadLockBugs();
    }

    private void assertNoBadLockBugs() {
        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
    }

    private void assertNumOfBugs(int number, String bugType) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().bugType(bugType).build();
        assertThat(getBugCollection(), containsExactly(number, bugTypeMatcher));
    }

    private void assertObjectBugExactly(String clazz, String method, String field) {
        BugInstanceMatcherBuilder bugInstanceMatcherBuilder = new BugInstanceMatcherBuilder().bugType(OBJECT_BUG).inClass(clazz).inMethod(method)
                .atField(field);
        final BugInstanceMatcher bugInstance = bugInstanceMatcherBuilder.build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

    private void assertMethodBugExactly(String clazz, String method, Set<String> exposingMethods) {
        BugInstanceMatcherBuilder bugInstanceMatcherBuilder = new BugInstanceMatcherBuilder().bugType(METHOD_BUG).inClass(clazz).inMethod(method);
        //        exposingMethods.forEach(bugInstanceMatcherBuilder::inMethod);

        final BugInstanceMatcher bugInstance = bugInstanceMatcherBuilder.build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

}
