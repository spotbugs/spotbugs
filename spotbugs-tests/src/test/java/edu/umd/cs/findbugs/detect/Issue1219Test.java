package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

class Issue1219Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue1219.class",
                "ghIssues/Issue1219$Parent.class",
                "ghIssues/Issue1219$Child.class",
                "ghIssues/Issue1219$Factory.class",
                "ghIssues/Issue1219$Test.class");

        assertBugCount("BC_UNCONFIRMED_CAST_OF_RETURN_VALUE", 1);
        assertBugAtLine("BC_UNCONFIRMED_CAST_OF_RETURN_VALUE", 36);
    }

    private void assertBugCount(String type, int expectedCount) {
        BugInstanceMatcher bugMatcher = new BugInstanceMatcherBuilder()
                .bugType(type).build();

        assertThat(getBugCollection(),
                containsExactly(expectedCount, bugMatcher));
    }

    private void assertBugAtLine(String type, int line) {
        BugInstanceMatcher bugMatcher = new BugInstanceMatcherBuilder()
                .bugType(type).atLine(line).build();

        assertThat(getBugCollection(), containsExactly(1, bugMatcher));
    }
}
