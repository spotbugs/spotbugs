package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/1472">GitHub issue #1472</a>
 */
public class Issue1472Test extends AbstractIntegrationTest {
    @Test
    public void test() {
        performAnalysis("ghIssues/Issue1472.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("SA_LOCAL_SELF_COMPUTATION")
                .build();
        assertThat(getBugCollection(), containsExactly(4, bugTypeMatcher));
    }
}
