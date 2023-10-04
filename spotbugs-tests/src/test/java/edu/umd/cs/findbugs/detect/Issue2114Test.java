package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

class Issue2114Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("../java11/Issue2114.class");

        assertFoundDefaultEncodingRelianceInMethod("filesReadString");
        assertFoundDefaultEncodingRelianceInMethod("filesReadAllLines");
        assertFoundDefaultEncodingRelianceInMethod("filesWriteString");
        assertFoundDefaultEncodingRelianceInMethod("filesNewBufferedReader");
        assertFoundDefaultEncodingRelianceInMethod("filesNewBufferedWriter");
    }

    private void assertFoundDefaultEncodingRelianceInMethod(String methodName) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("DM_DEFAULT_ENCODING")
                .inMethod(methodName)
                .build();

        assertThat(getBugCollection(), containsExactly(1, matcher));
    }
}
