package edu.umd.cs.findbugs.detect;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

public class Issue2147Test extends AbstractIntegrationTest {
    @Test
    public void test() {
        performAnalysis("ghIssues/Issue2147.class",
                "ghIssues/Issue2147A.class",
                "ghIssues/Issue2147B.class",
                "ghIssues/Issue2147C.class");
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("URF_UNREAD_FIELD").build();
        assertThat(getBugCollection(), containsExactly(0, matcher));
    }
}
