package edu.umd.cs.findbugs.ba;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

public class Issue893Test extends AbstractIntegrationTest {

    @Test
    public void test() {
        performAnalysis("ghIssues/Issue893.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }
}
