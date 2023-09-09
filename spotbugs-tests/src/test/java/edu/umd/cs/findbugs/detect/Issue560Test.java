package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;


import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

public class Issue560Test extends AbstractIntegrationTest {

    @Test
    public void test() {
        performAnalysis("ghIssues/Issue560.class",
                "ghIssues/Issue560$NotANestedTest.class",
                "ghIssues/Issue560$NestedTest.class");

        assertBugCount("SIC_INNER_SHOULD_BE_STATIC", 1);
        assertBugInClass("SIC_INNER_SHOULD_BE_STATIC", "ghIssues.Issue560$NotANestedTest");
    }

    public void assertBugCount(String type, int expectedCount) {
        BugInstanceMatcher bugMatcher = new BugInstanceMatcherBuilder()
                .bugType(type)
                .build();

        assertThat(getBugCollection(), containsExactly(expectedCount, bugMatcher));
    }

    public void assertBugInClass(String type, String className) {
        BugInstanceMatcher bugMatcher = new BugInstanceMatcherBuilder()
                .bugType(type)
                .inClass(className)
                .build();

        assertThat(getBugCollection(), containsExactly(1, bugMatcher));
    }
}
