package edu.umd.cs.findbugs.nullness;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/1839">GitHub Issue</a>
 */
class Issue1839Test extends AbstractIntegrationTest {

    @Test
    void test() {
        performAnalysis("../java17/Issue1839.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().bugType("NP_NONNULL_RETURN_VIOLATION").build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }
}
