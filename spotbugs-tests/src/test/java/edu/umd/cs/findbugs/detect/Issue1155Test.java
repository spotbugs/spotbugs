package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/1155">GitHub issue</a>
 */
public class Issue1155Test extends AbstractIntegrationTest {
    @Test
    public void testIssue1155() {
        performAnalysis("ghIssues/Issue1155.class");

        assertBugInMethodCount("NP_NULL_ON_SOME_PATH", "ghIssues.Issue1155", "demoOfCorrectNoReport", 0);
        assertBugInMethodCount("NP_NULL_ON_SOME_PATH", "ghIssues.Issue1155", "demoOfCorrectNoReportDifferentMethodName", 0);
        assertBugInMethodCount("NP_NULL_ON_SOME_PATH", "ghIssues.Issue1155", "demoOfInvalidReport", 0);

        assertThat(getBugCollection(), is(emptyIterable()));
    }
}
