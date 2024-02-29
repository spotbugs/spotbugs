package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

class Issue1877Test extends AbstractIntegrationTest {

    @Test
    @DisabledOnJre({ JRE.JAVA_8, JRE.JAVA_11 })
    void testIssue() {
        performAnalysis("../java17/Issue1877.class");

        assertBugCount("VA_FORMAT_STRING_USES_NEWLINE", 2);

        assertBugAtLine("VA_FORMAT_STRING_USES_NEWLINE", 18);
        assertBugAtLine("VA_FORMAT_STRING_USES_NEWLINE", 37);
    }

    private void assertBugCount(String type, int expectedCount) {
        BugInstanceMatcher bugMatcher = new BugInstanceMatcherBuilder()
                .bugType(type)
                .build();

        assertThat(getBugCollection(), containsExactly(expectedCount, bugMatcher));
    }

    private void assertBugAtLine(String type, int line) {
        BugInstanceMatcher bugMatcher = new BugInstanceMatcherBuilder()
                .bugType(type)
                .atLine(line)
                .build();

        assertThat(getBugCollection(), containsExactly(1, bugMatcher));
    }
}
