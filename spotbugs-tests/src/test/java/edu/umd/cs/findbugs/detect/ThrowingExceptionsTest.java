package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class ThrowingExceptionsTest extends AbstractIntegrationTest {
    @Test
    public void throwingExceptionsTests() {
        performAnalysis("MethodsThrowingExceptions.class");

        assertNumOfTHROWSBugs("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", 1);
        assertNumOfTHROWSBugs("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", 1);
        assertNumOfTHROWSBugs("THROWS_METHOD_THROWS_CLAUSE_THROWABLE", 1);

        assertNumOfTHROWSBugs("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", 1, "isCapitalizedThrowingRuntimeException");
        assertNumOfTHROWSBugs("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", 0, "isCapitalizedThrowingSpecializedException");

        assertNumOfTHROWSBugs("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", 1, "methodThrowingBasicException");
        assertNumOfTHROWSBugs("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", 0, "methodThrowingIOException");

        assertNumOfTHROWSBugs("THROWS_METHOD_THROWS_CLAUSE_THROWABLE", 1, "methodThrowingThrowable");
    }

    @Test
    public void testNestedInterface() {
        performAnalysis("MethodsThrowingExceptions$ThrowThrowable.class");

        assertNumOfTHROWSBugs("THROWS_METHOD_THROWS_CLAUSE_THROWABLE", 1);
    }

    @SuppressWarnings("SameParameterValue")
    private void assertNumOfTHROWSBugs(String bugType, int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertNumOfTHROWSBugs(String bugType, int num, String method) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inClass("MethodsThrowingExceptions")
                .inMethod(method)
                .build();
        if (num > 0) {
            assertThat(getBugCollection(), hasItem(bugTypeMatcher));
        }
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }
}
