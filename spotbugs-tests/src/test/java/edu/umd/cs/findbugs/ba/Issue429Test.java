package edu.umd.cs.findbugs.ba;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

/**
 * Unit test to reproduce <a href="https://github.com/spotbugs/spotbugs/issues/429">#429</a>.
 */
class Issue429Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue429.class");
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder().bugType("RV_RETURN_VALUE_IGNORED")
                .build();
        assertThat(getBugCollection(), containsExactly(0, matcher));
    }
}
