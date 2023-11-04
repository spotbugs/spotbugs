package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Function;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class FindSynchronizationLockTest extends AbstractIntegrationTest {
    private static final String METHOD_BUG = "PFL_BAD_METHOD_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";
    private static final String OBJECT_BUG = "PFL_BAD_OBJECT_SYNCHRONIZATION_USE_PRIVATE_FINAL_LOCK_OBJECTS";

    @Disabled
    @Test
    void testBadMethodSynchronizationLock() {
        performAnalysis("privateFinalLocks/BadMethodSynchronizationLock.class",
                "privateFinalLocks/ClassExposingItSelf.class",
                "privateFinalLocks/ClassExposingACollectionOfItself.class",
                "privateFinalLocks/ClassExposingAMapOfItself.class"
        );

        assertNumOfBugs(3);

        assertBugExactly(METHOD_BUG, "privateFinalLocks.ClassExposingItSelf", "doStuff", Optional.empty());
        assertBugExactly(METHOD_BUG, "privateFinalLocks.ClassExposingACollectionOfItself", "doStuff", Optional.empty());
        assertBugExactly(METHOD_BUG, "privateFinalLocks.ClassExposingAMapOfItself", "doStuff", Optional.empty());
    }

    @Disabled
    @Test
    public void testBadMethodSynchronizationWithPublicStaticLockObject() {
        performAnalysis("privateFinalLocks/BadMethodSynchronizationWithPublicStaticLock.class",
                "privateFinalLocks/SomeOtherClass.class");

        assertNumOfBugs(1);

        assertBugExactly(METHOD_BUG, "privateFinalLocks.SomeOtherClass", "changeValue", Optional.empty());
    }

    @Test
    void goodMethodSynchronizationLock() {
        performAnalysis("privateFinalLocks/GoodMethodSynchronizationLock.class",
                "privateFinalLocks/GoodMethodSynchronizationLock.class",
                "privateFinalLocks/GoodMethodSynchronizationLock.class",
                "privateFinalLocks/GoodMethodSynchronizationLock.class"
        );

        assertNumOfBugs(0);
    }

    @Test
    public void testBadSynchronizationWithPublicFinalLockObject() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithPublicFinalLock.class");

        assertNumOfBugs(1);

        assertBugExactly(OBJECT_BUG, "privateFinalLocks.BadSynchronizationWithPublicFinalLock", "changeValue", Optional.of("lock"));
    }

    @Test
    public void testBadSynchronizationWithPubliclyAccessibleNonFinalLockObject() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithPubliclyAccessibleNonFinalLock.class");

        assertNumOfBugs(1);

        assertBugExactly(OBJECT_BUG, "privateFinalLocks.BadSynchronizationWithPubliclyAccessibleNonFinalLock", "changeValue", Optional.of("lock"));
    }

    @Test
    public void testBadSynchronizationWithPublicNonFinalLockObject() {
        performAnalysis("privateFinalLocks/BadSynchronizationWithPublicNonFinalLock.class");

        assertNumOfBugs(1);

        assertBugExactly(OBJECT_BUG, "privateFinalLocks.BadSynchronizationWithPublicNonFinalLock", "changeValue", Optional.of("lock"));
    }

    @Test
    public void testGoodSynchronizationWithPrivateFinalLockObject() {
        performAnalysis("privateFinalLocks/GoodSynchronizationWithPrivateFinalLock.class");

        assertNumOfBugs(0);
    }

    @Test
    public void testGoodSynchronizationWithPrivateStaticLockObject() {
        performAnalysis("privateFinalLocks/GoodSynchronizationWithPrivateStaticLock.class");

        assertNumOfBugs(0);
    }

    private void assertNumOfBugs(int number) {
        BugInstanceMatcher methodBugMatcher = new BugInstanceMatcherBuilder().bugType(METHOD_BUG).build();
        BugInstanceMatcher objectBugMatcher = new BugInstanceMatcherBuilder().bugType(OBJECT_BUG).build();
        long nrMethodBugs = getBugCollection().getCollection().stream().filter(methodBugMatcher::matches).count();
        long nrObjectBugs = getBugCollection().getCollection().stream().filter(objectBugMatcher::matches).count();

        assertEquals(number, nrMethodBugs + nrObjectBugs);
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
        final BugInstanceMatcher bugInstance = bugInstanceMatcherBuilder.build();

        assertThat(getBugCollection(), contains(bugInstance));
    }

}
