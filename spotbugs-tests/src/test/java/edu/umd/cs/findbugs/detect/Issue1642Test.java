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
public class Issue1642Test extends AbstractIntegrationTest {
    @Test
    public void test() {
        performAnalysis("ghIssues/Issue1642.class");
        BugInstanceMatcherBuilder builder = new BugInstanceMatcherBuilder()
                .bugType("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR");

        BugInstanceMatcher fieldBMatcher = builder.atField("b").build();
        BugInstanceMatcher fieldCMatcher = builder.atField("c").build();
        BugInstanceMatcher fieldDMatcher = builder.atField("d").build();
        BugInstanceMatcher fieldXMatcher = builder.atField("x").build();
        BugInstanceMatcher fieldYMatcher = builder.atField("y").build();

        assertThat(getBugCollection(), containsExactly(1, fieldBMatcher));
        assertThat(getBugCollection(), containsExactly(1, fieldCMatcher));
        assertThat(getBugCollection(), containsExactly(1, fieldDMatcher));
        assertThat(getBugCollection(), containsExactly(1, fieldXMatcher));
        // y is matched twice, once on the field and once in the constructor
        assertThat(getBugCollection(), containsExactly(2, fieldYMatcher));
    }
}
