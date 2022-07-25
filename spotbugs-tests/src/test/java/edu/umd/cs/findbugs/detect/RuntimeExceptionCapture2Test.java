package edu.umd.cs.findbugs.detect;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;

public class RuntimeExceptionCapture2Test extends AbstractIntegrationTest {
    @Test
    public void test() {
        performAnalysis("REC2Test.class",
                "REC2Test$Resource.class",
                "REC2Test$TestPass5.class",
                "REC2Test$Unknown.class",
                "REC2Test$1.class");

        fail("testFail", 58, "REC2_CATCH_RUNTIME_EXCEPTION", Confidence.LOW);
        fail("testFail", 60, "REC2_CATCH_EXCEPTION", Confidence.LOW);
        fail("testFail2", 71, "REC2_CATCH_RUNTIME_EXCEPTION", Confidence.LOW);
        fail("testFail3", 83, "REC2_CATCH_THROWABLE", Confidence.LOW);

        pass("REC2Test", "testPass", "REC2_CATCH_RUNTIME_EXCEPTION");
        pass("REC2Test", "testPass", "REC2_CATCH_EXCEPTION");
        pass("REC2Test", "testPass2", "REC2_CATCH_THROWABLE");
        pass("REC2Test", "testPass3", "REC2_CATCH_THROWABLE");
        pass("REC2Test", "testPass4", "REC2_CATCH_RUNTIME_EXCEPTION");
        pass("REC2Test$TestPass5", "testPass5", "REC2_CATCH_RUNTIME_EXCEPTION");
    }

    private void fail(String methodName, int line, String exceptionName, Confidence confidence) {
        countBugs(methodName, exceptionName, 1);
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(exceptionName)
                .inClass("REC2Test")
                .inMethod(methodName)
                .withConfidence(confidence)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

    private void pass(String className, String methodName, String exceptionName) {
        countBugs(methodName, exceptionName, 0);
    }

    private void countBugs(String methodName, String exceptionName, int count) {
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(exceptionName)
                .inClass("REC2Test")
                .inMethod(methodName)
                .build();
        assertThat(getBugCollection(), containsExactly(count, bugTypeMatcher));
    }
}
