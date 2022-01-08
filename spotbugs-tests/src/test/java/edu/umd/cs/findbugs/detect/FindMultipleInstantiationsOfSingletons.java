package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class FindMultipleInstantiationsOfSingletons extends AbstractIntegrationTest {
    @Test
    public void cloneableSingletonTest() {
        performAnalysis("singletons/CloneableSingleton.class");
        assertNumOfSINGBugs("SING_SINGLETON_IMPLEMENTS_CLONEABLE", 1);
        assertSINGBug("SING_SINGLETON_IMPLEMENTS_CLONEABLE", "CloneableSingleton", "clone");
    }

    @Test
    public void defaultConstructorTest() {
        performAnalysis("singletons/DefaultConstructor.class");
        assertNumOfSINGBugs("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", 1);
        assertSINGBug("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", "DefaultConstructor");
    }

    @Test
    public void protectedConstructorTest() {
        performAnalysis("singletons/ProtectedConstructor.class");
        assertNumOfSINGBugs("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", 1);
        assertSINGBug("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", "ProtectedConstructor");
    }

    @Test
    public void notCloneableButHasCloneMethodTest() {
        performAnalysis("singletons/NotCloneableButHasCloneMethod.class");
        assertNumOfSINGBugs("SING_SINGLETON_IMPLEMENTS_CLONE_METHOD", 1);
        assertSINGBug("SING_SINGLETON_IMPLEMENTS_CLONE_METHOD", "NotCloneableButHasCloneMethod", "clone");
    }

    @Test
    public void serializableSingletonTest() {
        performAnalysis("singletons/SerializableSingleton.class");
        assertNumOfSINGBugs("SING_SINGLETON_IMPLEMENTS_SERIALIZEABLE", 1);
        assertSINGBug("SING_SINGLETON_IMPLEMENTS_SERIALIZEABLE", "SerializableSingleton");
    }

    @Test
    public void notSynchronizedSingletonTest() {
        performAnalysis("singletons/NotSynchronizedSingleton.class");
        assertNumOfSINGBugs("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED", 1);
        assertSINGBug("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED", "NotSynchronizedSingleton", "getInstance");
    }

    @Test
    public void inappropriateSynchronization() {
        performAnalysis("singletons/InappropriateSynchronization.class");
        // now cannot detect synchronization bug 
        // because of the using of a monitor inside function
        assertNumOfSINGBugs("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED", 0);
        //assertSINGBug("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED", "InappropriateSynchronization", "getInstance");
    }

    @Test
    public void appropriateSingletonTest() {
        performAnalysis("singletons/AppropriateSingleton.class");
        assertNumOfSINGBugs("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", 0);
        assertNumOfSINGBugs("SING_SINGLETON_IMPLEMENTS_CLONEABLE", 0);
        assertNumOfSINGBugs("SING_SINGLETON_INDIRECTLY_IMPLEMENTS_CLONEABLE", 0);
        assertNumOfSINGBugs("SING_SINGLETON_IMPLEMENTS_CLONE_METHOD", 0);
        assertNumOfSINGBugs("SING_SINGLETON_IMPLEMENTS_SERIALIZEABLE", 0);
        assertNumOfSINGBugs("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED", 0);
    }

    @Test
    public void doubleCheckedLockingTest() {
        performAnalysis("singletons/DoubleCheckedLocking.class");
        assertNumOfSINGBugs("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED", 0);
    }

    @Test
    public void multiplePrivateConstructorsTest() {
        performAnalysis("singletons/MultiplePrivateConstructors.class");
        assertNumOfSINGBugs("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", 0);
    }

    @Test
    public void multipleNonPrivateConstructorsTest() {
        performAnalysis("singletons/MultipleNonPrivateConstructors.class");
        assertNumOfSINGBugs("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", 1);
        assertSINGBug("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", "MultipleNonPrivateConstructors");
    }

    @Test
    public void multipleNonPrivateConstructorsTest2() {
        performAnalysis("singletons/MultipleNonPrivateConstructors2.class");
        assertNumOfSINGBugs("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", 1);
        assertSINGBug("SING_SINGLETON_HAS_NONPRIVATE_CONSTRUCTOR", "MultipleNonPrivateConstructors2");
    }

    private void assertNumOfSINGBugs(String bugType, int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertSINGBug(String bugType, String className) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inClass(className)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

    private void assertSINGBug(String bugType, String className, String method) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inClass(className)
                .inMethod(method)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }
}
