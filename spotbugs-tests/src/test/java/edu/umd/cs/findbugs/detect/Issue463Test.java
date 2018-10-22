package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/463">GitHub issue</a>
 */
public class Issue463Test extends AbstractIntegrationTest {
    @Test
    public void testAnnotatedClass() {
        performAnalysis("ghIssues/Issue463.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().bugType("RV_RETURN_VALUE_IGNORED")
                .atLine(37).build();
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher));
    }

    /**
     * When package is annotated with {@code @CheckReturnValue} and method is not annotated with
     * {@code @CanIgnoreReturnValue}, SpotBugs should report a bug for invocation which doesn't use returned value.
     *
     * @see <a href="https://github.com/spotbugs/spotbugs/issues/582">Issue 582</a>
     */
    @Test
    public void testAnnotatedPackage() {
        performAnalysis("ghIssues/issue463/Issue463.class", "ghIssues/issue463/package-info.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().bugType("RV_RETURN_VALUE_IGNORED")
                .atLine(34).build();
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher));
    }
}
