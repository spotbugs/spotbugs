
package edu.umd.cs.findbugs.detect.MyTest;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.junit.Assert.assertThat;
/**
 * @author Chen
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/374">GitHub issue</a>
 */
public class Issue374Test extends AbstractIntegrationTest {
    /**
     * When a package, method or class is annotated with
     * {@code @ParametersAreNonnullByDefault}, SpotBugs should
     * report a bug for parameters that must be non null but
     * be marked as nullable.
     */
    @Test
    public void test() {
        performAnalysis(
                "ghIssues/issue374/package-info.class",
                "ghIssues/issue374/ClassLevel.class",
                "ghIssues/issue374/MethodLevel.class",
                "ghIssues/issue374/PackageLevel.class"
        );
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
                .build();
        assertThat(getBugCollection(), containsExactly(3, matcher));
    }
}