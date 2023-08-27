package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;


public class Issue2465Test extends AbstractIntegrationTest {
    @Test
    public void test() {
        performAnalysis("ghIssues/Issue2465.class");

        // Due to issue #2465 the analysis should fail and we should not get further
        // Check that with the fix we get not bug instead of a crash
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .build();

        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }
}
