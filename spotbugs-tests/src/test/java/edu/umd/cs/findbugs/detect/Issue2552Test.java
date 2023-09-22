package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;


public class Issue2552Test extends AbstractIntegrationTest {
    @Test
    public void test() {
        performAnalysis("../java14/ghIssues/Issue2552.class");

        assertBugCount("EI_EXPOSE_REP", 1);

        assertBugAtLine("EI_EXPOSE_REP", 12);
    }

    public void assertBugCount(String type, int expectedCount) {
        BugInstanceMatcher bugMatcher = new BugInstanceMatcherBuilder()
                .bugType(type)
                .build();

        assertThat(getBugCollection(), containsExactly(expectedCount, bugMatcher));
    }

    public void assertBugAtLine(String type, int line) {
        BugInstanceMatcher bugMatcher = new BugInstanceMatcherBuilder()
                .bugType(type)
                .atLine(line)
                .build();

        assertThat(getBugCollection(), containsExactly(1, bugMatcher));
    }
}
