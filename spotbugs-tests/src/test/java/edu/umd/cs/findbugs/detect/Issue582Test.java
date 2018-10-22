package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

/**
 * SpotBugs should support both of {@code javax.annotation.CheckReturnValue} and
 * {@code com.google.errorprone.annotations.CheckReturnValue} annotations.
 *
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/582">GitHub issue</a>
 * @since 3.1.3
 */
public class Issue582Test extends AbstractIntegrationTest {
    @Test
    public void testAnnnotatedClass() {
        performAnalysis("ghIssues/Issue582.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().bugType("RV_RETURN_VALUE_IGNORED")
                .atLine(35).build();
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher));
    }

    @Test
    public void testAnnotatedPackage() {
        performAnalysis("ghIssues/issue582/Issue582.class", "ghIssues/issue582/package-info.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().bugType("RV_RETURN_VALUE_IGNORED")
                .atLine(34).build();
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher));
    }
}
