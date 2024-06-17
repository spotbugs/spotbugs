package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

class ThrowingExceptionsTest extends AbstractIntegrationTest {

    @Test
    void throwingExceptionsTests() {
        performAnalysis("MethodsThrowingExceptions.class", "MethodsThrowingExceptions$ThrowThrowable.class",
                "MethodsThrowingExceptions$1.class", "MethodsThrowingExceptions$2.class");

        assertNumOfTHROWSBugs("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", 1);
        assertNumOfTHROWSBugs("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", 1);
        assertNumOfTHROWSBugs("THROWS_METHOD_THROWS_CLAUSE_THROWABLE", 2);

        assertNumOfTHROWSBugs("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", "MethodsThrowingExceptions", 1, "isCapitalizedThrowingRuntimeException");
        assertNumOfTHROWSBugs("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", "MethodsThrowingExceptions", 0, "isCapitalizedThrowingSpecializedException");

        assertNumOfTHROWSBugs("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", "MethodsThrowingExceptions", 1, "methodThrowingBasicException");
        assertNumOfTHROWSBugs("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", "MethodsThrowingExceptions", 0, "methodThrowingIOException");

        assertNumOfTHROWSBugs("THROWS_METHOD_THROWS_CLAUSE_THROWABLE", "MethodsThrowingExceptions", 1, "methodThrowingThrowable");
        assertNumOfTHROWSBugs("THROWS_METHOD_THROWS_CLAUSE_THROWABLE", "MethodsThrowingExceptions$ThrowThrowable", 1, "run");
    }

    @SuppressWarnings("SameParameterValue")
    private void assertNumOfTHROWSBugs(String bugType, int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertNumOfTHROWSBugs(String bugType, String className, int num, String method) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .inClass(className)
                .inMethod(method)
                .build();
        if (num > 0) {
            assertThat(getBugCollection(), hasItem(bugTypeMatcher));
        }
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }
}
