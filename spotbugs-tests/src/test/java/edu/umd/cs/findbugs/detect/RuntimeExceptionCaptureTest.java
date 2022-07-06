package edu.umd.cs.findbugs.detect;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;

public class RuntimeExceptionCaptureTest extends AbstractIntegrationTest {
    @Test
    public void test() {
        performAnalysis("RECTest.class");

        fail("testFail", 46, Confidence.MEDIUM);
        fail("testFail2", 58, Confidence.MEDIUM);
        fail("testFail3", 71, Confidence.MEDIUM);
        fail("testFail4", 83, Confidence.MEDIUM);
        fail("testFail5", 95, Confidence.MEDIUM);
        fail("testFail6", 108, Confidence.MEDIUM);
        fail("testFail7", 123, Confidence.LOW);

        pass("testPass");
        pass("testPass2");
        pass("testPass3");
        pass("testPass4");
        pass("testPass5");
        pass("testPass6");
        pass("testPass7");
    }

    private void fail(String methodName, int line, Confidence confidence) {
        countBugs(methodName, 1);
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("REC_CATCH_EXCEPTION")
                .inClass("RECTest")
                .inMethod(methodName)
                .withConfidence(confidence)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

    private void pass(String methodName) {
        countBugs(methodName, 0);
    }

    private void countBugs(String methodName, int count) {
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("REC_CATCH_EXCEPTION")
                .inClass("RECTest")
                .inMethod(methodName)
                .build();
        assertThat(getBugCollection(), containsExactly(count, bugTypeMatcher));
    }
}
