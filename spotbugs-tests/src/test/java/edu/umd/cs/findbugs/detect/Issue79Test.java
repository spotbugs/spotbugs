package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * SpotBugs should remove the ResultSet obligation from all set
 * when one occurrence of that type of obligation is Statement
 * from all states.
 *
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/79">GitHub issue</a>
 * @since 4.1.3
 */
public class Issue79Test extends AbstractIntegrationTest {

    @Test
    public void test() {
        performAnalysis("ghIssues/Issue79.class");
        BugInstanceMatcher bugMatcher = new BugInstanceMatcherBuilder().build();
        assertThat(getBugCollection(), containsExactly(0, bugMatcher));
    }
}
