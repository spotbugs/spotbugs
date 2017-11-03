package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/484">GitHub issue</a>
 * @since 3.1
 */
public class Issue484Test extends AbstractIntegrationTest {
    @Test
    public void test() {
        performAnalysis("ghIssues/Issue484.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().bugType("NP_NONNULL_PARAM_VIOLATION")
                .build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }

}
