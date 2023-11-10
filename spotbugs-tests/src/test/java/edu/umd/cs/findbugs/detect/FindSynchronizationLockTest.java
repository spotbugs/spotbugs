package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

public class FindSynchronizationLockTest extends AbstractIntegrationTest {
    /**
     * @note: Add tests
     * - include inheritance in some cases
     * - create multiple bad method synchronizations inside the same class to check if there is no overlap
     */
    private static final String METHOD_BUG = "PFL_BAD_METHOD_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    private static final String OBJECT_BUG = "PFL_BAD_OBJECT_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";

    @Test
    void testBadMethodSynchronizationLock() {
        performAnalysis("privateFinalLocks/BadMethodSynchronizationLock.class",
                "privateFinalLocks/ClassExposingItSelf.class",
                "privateFinalLocks/ClassExposingACollectionOfItself.class",
                "privateFinalLocks/ClassExposingAMapOfItself.class"
        );

        assertNumOfBugs(4, METHOD_BUG);

        assertBugExactly(METHOD_BUG, "privateFinalLocks.ClassExposingItSelf", "doStuff", Optional.empty());
        assertBugExactly(METHOD_BUG, "privateFinalLocks.ClassExposingItSelf", "changeValue", Optional.empty());
        assertBugExactly(METHOD_BUG, "privateFinalLocks.ClassExposingACollectionOfItself", "doStuff", Optional.empty());
        assertBugExactly(METHOD_BUG, "privateFinalLocks.ClassExposingAMapOfItself", "doStuff", Optional.empty());
    }

    @Test
    public void testBadMethodSynchronizationWithPublicStaticLockObject() {
        performAnalysis("privateFinalLocks/BadMethodSynchronizationWithPublicStaticLock.class",
                "privateFinalLocks/SomeOtherClass.class");

        assertNumOfBugs(1, METHOD_BUG);

        assertBugExactly(METHOD_BUG, "privateFinalLocks.SomeOtherClass", "changeValue", Optional.empty());
    }

    @Test
    public void testBadSynchronizationLockAcquiredFromParent() {
        performAnalysis("privateFinalLocks/BadSynchronizationLockBase.class",
                "privateFinalLocks/BadSynchronizationWithLocksFromBase.class",
                "privateFinalLocks/BadSynchronizationLockBaseWithMultipleMethods.class");

        assertNumOfBugs(5, OBJECT_BUG);

        assertBugExactly(OBJECT_BUG, "privateFinalLocks.BadSynchronizationLockBase", "doStuff", Optional.of("baseLock"));
        assertBugExactly(OBJECT_BUG, "privateFinalLocks.BadSynchronizationWithLocksFromBase", "doOtherStuff", Optional.of("baseLock"));
        assertBugExactly(OBJECT_BUG, "privateFinalLocks.BadSynchronizationWithLocksFromBase", "changeValue", Optional.of("lock"));
        assertBugExactly(OBJECT_BUG, "privateFinalLocks.BadSynchronizationLockBaseWithMultipleMethods", "doStuff", Optional.of("baseLock"));
        assertBugExactly(OBJECT_BUG, "privateFinalLocks.BadSynchronizationLockBaseWithMultipleMethods", "doOtherStuff", Optional.of("baseLock"));
    }

    @Test
    void goodMethodSynchronizationLock() {
        performAnalysis("privateFinalLocks/GoodMethodSynchronizationLock.class",
                "privateFinalLocks/GoodMethodSynchronizationLock.class",
                "privateFinalLocks/GoodMethodSynchronizationLock.class",
                "privateFinalLocks/GoodMethodSynchronizationLock.class"
        );

        assertNoBadLockBugs();
    }

    @Test
    public void testBadSynchronizationWithAccessibleFinalLockObject() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithPublicFinalLock.class",
                "privateFinalLocks/BadSynchronizationWithPublicFinalLockFromParent.class");

        assertNumOfBugs(3, OBJECT_BUG);

        assertBugExactly(OBJECT_BUG, "privateFinalLocks.BadSynchronizationWithPublicFinalLock", "doSomeStuff", Optional.of("baseLock"));
        assertBugExactly(OBJECT_BUG, "privateFinalLocks.BadSynchronizationWithPublicFinalLockFromParent", "doSomeStuff2", Optional.of("baseLock"));
        assertBugExactly(OBJECT_BUG, "privateFinalLocks.BadSynchronizationWithPublicFinalLockFromParent", "doSomeStuff3", Optional.of("lock"));
    }

    @Test
    public void testBadSynchronizationWithPubliclyAccessibleNonFinalLockObject() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithPubliclyAccessibleNonFinalLock.class",
                "privateFinalLocks/BadSynchronizationWithPubliclyAccessibleNonFinalLockFromParent.class"
        );

        assertNumOfBugs(2, OBJECT_BUG);

        assertBugExactly(OBJECT_BUG, "privateFinalLocks.BadSynchronizationWithPubliclyAccessibleNonFinalLock", "doSomeStuff", Optional.of("baseLock"));
        assertBugExactly(OBJECT_BUG, "privateFinalLocks.BadSynchronizationWithPubliclyAccessibleNonFinalLockFromParent", "doSomeStuff2", Optional.of("lock"));
    }

    @Test
    public void testBadSynchronizationWithPublicNonFinalLockObject() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithPublicNonFinalLock.class",
                "privateFinalLocks/BadSynchronizationWithNonFinalLockFromParent.class"
        );

        assertNumOfBugs(3, OBJECT_BUG);

        assertBugExactly(OBJECT_BUG, "privateFinalLocks.BadSynchronizationWithPublicNonFinalLock", "doSomeStuff", Optional.of("baseLock"));
        assertBugExactly(OBJECT_BUG, "privateFinalLocks.BadSynchronizationWithNonFinalLockFromParent", "doSomeStuff2", Optional.of("baseLock"));
        assertBugExactly(OBJECT_BUG, "privateFinalLocks.BadSynchronizationWithNonFinalLockFromParent", "doSomeStuff3", Optional.of("lock"));
    }

    @Test
    public void testGoodSynchronizationWithPrivateFinalLockObject() {
        performAnalysis("privateFinalLocks/GoodSynchronizationWithPrivateFinalLock.class");

        assertNoBadLockBugs();
    }

    @Test
    public void testGoodSynchronizationWithPrivateStaticLockObject() {
        performAnalysis("privateFinalLocks/GoodSynchronizationWithPrivateStaticLock.class");

        assertNoBadLockBugs();
    }

    @Test
    public void testGoodSynchronizationWithInheritance() {
        performAnalysis("privateFinalLocks/GoodSynchronizationWithInheritance.class",
                "privateFinalLocks/GoodSynchronizationInheritedFromParent.class");

        assertNoBadLockBugs();
    }

    private void assertNoBadLockBugs() {
        assertNumOfBugs(0, METHOD_BUG);
        assertNumOfBugs(0, OBJECT_BUG);
    }

    private void assertNumOfBugs(int number, String bugType) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType).build();
        assertThat(getBugCollection(), containsExactly(number, bugTypeMatcher));
    }

    private void assertBugExactly(String bugType,
                                  String clazz,
                                  String method,
                                  Optional<String> fieldOpt) {
        BugInstanceMatcherBuilder bugInstanceMatcherBuilder = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inClass(clazz)
                .inMethod(method);
        fieldOpt.ifPresent(bugInstanceMatcherBuilder::atField);
        final BugInstanceMatcher bugInstance =
                bugInstanceMatcherBuilder.build();

        assertThat(getBugCollection(), hasItem(bugInstance));
    }

}
