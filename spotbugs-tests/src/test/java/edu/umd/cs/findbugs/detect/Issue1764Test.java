package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

import org.junit.jupiter.api.Test;
import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/1764">GitHub issue</a>
 */
class Issue1764Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        System.setProperty("frc.debug", "true");
        performAnalysis("ghIssues/Issue1764.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("ES_COMPARING_STRINGS_WITH_EQ")
                .atLine(7)
                .build();
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher));
    }
}
