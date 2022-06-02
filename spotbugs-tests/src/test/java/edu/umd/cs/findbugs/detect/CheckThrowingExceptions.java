package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Ignore;
import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class CheckThrowingExceptions extends AbstractIntegrationTest {
    @Ignore("Detector disabled, see issue #2040")
    @Test
    public void throwingExceptionsTests() {
        performAnalysis("MethodsThrowingExceptions.class");

        assertNumOfTHROWSBugs("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", 1);
        assertNumOfTHROWSBugs("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", 1);
        assertNumOfTHROWSBugs("THROWS_METHOD_THROWS_CLAUSE_THROWABLE", 1);

        assertNumOfTHROWSBugs("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", 1, "MethodsThrowingExceptions", "isCapitalizedThrowingRuntimeException");
        assertNumOfTHROWSBugs("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", 0, "MethodsThrowingExceptions", "isCapitalizedThrowingSpecializedException");

        assertNumOfTHROWSBugs("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", 1, "MethodsThrowingExceptions", "methodThrowingBasicException");
        assertNumOfTHROWSBugs("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", 0, "MethodsThrowingExceptions", "methodThrowingIOException");

        assertNumOfTHROWSBugs("THROWS_METHOD_THROWS_CLAUSE_THROWABLE", 1, "MethodsThrowingExceptions", "methodThrowingThrowable");
    }

    private void assertNumOfTHROWSBugs(String bugType, int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(bugType)
                .build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertNumOfTHROWSBugs(String bugType, int num, String className, String method) {
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
