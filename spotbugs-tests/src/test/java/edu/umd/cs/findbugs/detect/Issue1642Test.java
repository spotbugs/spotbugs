package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/1472">GitHub issue #1472</a>
 */
class Issue1642Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue1642.class");
        assertBugTypeCount("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", 6);

        assertBugAtField("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", "Issue1642", "b");
        assertBugAtField("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", "Issue1642", "c");
        assertBugAtField("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", "Issue1642", "d");
        assertBugAtField("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR", "Issue1642", "x");

        BugInstanceMatcher fieldYMatcher = new BugInstanceMatcherBuilder()
                .bugType("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR")
                .atField("y").build();
        // y is matched twice, once on the field and once in the constructor
        assertThat(getBugCollection(), containsExactly(2, fieldYMatcher));
    }
}
