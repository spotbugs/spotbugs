package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

class Issue2114Test extends AbstractIntegrationTest {

    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testIssue() {
        performAnalysis("../java11/Issue2114.class");

        assertDidNotFindDefaultEncodingRelianceInMethod("filesReadString");
        assertDidNotFindDefaultEncodingRelianceInMethod("filesReadAllLines");
        assertDidNotFindDefaultEncodingRelianceInMethod("filesWriteString");
        assertDidNotFindDefaultEncodingRelianceInMethod("filesNewBufferedReader");
        assertDidNotFindDefaultEncodingRelianceInMethod("filesNewBufferedWriter");
    }

    private void assertDidNotFindDefaultEncodingRelianceInMethod(String methodName) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("DM_DEFAULT_ENCODING")
                .inMethod(methodName)
                .build();

        assertThat(getBugCollection(), containsExactly(0, matcher));
    }
}
