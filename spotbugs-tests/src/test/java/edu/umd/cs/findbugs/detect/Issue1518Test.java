package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/1518">GitHub issue #1518</a>
 */
public class Issue1518Test extends AbstractIntegrationTest {
    @Test
    public void test() {
        performAnalysis("ghIssues/Issue1518.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("RV_01_TO_INT")
                .build();
        assertThat(getBugCollection(), containsExactly(3, bugTypeMatcher));
    }
}
