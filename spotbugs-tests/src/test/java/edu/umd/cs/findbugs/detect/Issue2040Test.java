package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

public class Issue2040Test extends AbstractIntegrationTest {
    @Test
    public void test() {
        performAnalysis("ghIssues/issue2040/Base.class",
                "ghIssues/issue2040/Derived.class",
                "ghIssues/issue2040/GenericBase.class",
                "ghIssues/issue2040/GenericDerived.class",
                "ghIssues/issue2040/Interface.class",
                "ghIssues/issue2040/Generic.class",
                "ghIssues/issue2040/Caller.class");

        assertNumOfBugs("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", 2);
        assertBugInMethod("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", "Derived", "runtimeExThrownFromCatch");
        assertBugInMethod("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", "Derived", "runtimeExceptionThrowingMethod");

        assertNumOfBugs("THROWS_METHOD_THROWS_CLAUSE_THROWABLE", 4);
        assertBugInMethod("THROWS_METHOD_THROWS_CLAUSE_THROWABLE", "Base", "method");
        assertBugInMethod("THROWS_METHOD_THROWS_CLAUSE_THROWABLE", "Derived", "throwableThrowingMethod");
        assertBugInMethod("THROWS_METHOD_THROWS_CLAUSE_THROWABLE", "GenericBase", "method1");
        assertBugInMethod("THROWS_METHOD_THROWS_CLAUSE_THROWABLE", "GenericBase", "method2");

        assertNumOfBugs("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", 5);
        assertBugInMethod("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", "Derived", "exceptionThrowingMethod");
        assertBugInMethod("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", "Derived", "exThrownFromCatch");
        assertBugInMethod("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", "GenericBase", "method");
        assertBugInMethod("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", "GenericBase", "method2");
        assertBugInMethod("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", "Interface", "iMethod");
    }

    private void assertNumOfBugs(String bugtype, int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype).build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertBugInMethod(String bugType, String className, String method) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inClass(className)
                .inMethod(method)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }
}
