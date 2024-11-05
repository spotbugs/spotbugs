package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/463">GitHub issue</a>
 */
class Issue463Test extends AbstractIntegrationTest {

    @Test
    void testAnnotatedClass() {
        performAnalysis("ghIssues/Issue463.class");
        assertBugAtLine("RV_RETURN_VALUE_IGNORED", 37);
    }

    /**
     * When package is annotated with {@code @CheckReturnValue} and method is not annotated with
     * {@code @CanIgnoreReturnValue}, SpotBugs should report a bug for invocation which doesn't use returned value.
     *
     * @see <a href="https://github.com/spotbugs/spotbugs/issues/582">Issue 582</a>
     */
    @Test
    void testAnnotatedPackage() {
        performAnalysis("ghIssues/issue463/Issue463.class", "ghIssues/issue463/package-info.class");
        assertBugAtLine("RV_RETURN_VALUE_IGNORED", 34);
    }
}
