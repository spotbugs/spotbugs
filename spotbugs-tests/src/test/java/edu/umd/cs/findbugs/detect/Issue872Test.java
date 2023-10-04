package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

class Issue872Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue872.class",
                "ghIssues/Issue872$Value.class");

        assertBugCount("RV_RETURN_VALUE_IGNORED", 1);
        assertBugCount("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", 1);

        assertBugAtLine("RV_RETURN_VALUE_IGNORED", 12);
        assertBugAtLine("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", 18);
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
