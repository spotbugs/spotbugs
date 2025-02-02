package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

/**
 * SpotBugs should support both of {@code javax.annotation.CheckReturnValue} and
 * {@code com.google.errorprone.annotations.CheckReturnValue} annotations.
 *
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/582">GitHub issue</a>
 * @since 3.1.3
 */
class Issue582Test extends AbstractIntegrationTest {

    @Test
    void testAnnnotatedClass() {
        performAnalysis("ghIssues/Issue582.class");
        assertBugAtLine("RV_RETURN_VALUE_IGNORED", 35);
    }

    @Test
    void testAnnotatedPackage() {
        performAnalysis("ghIssues/issue582/Issue582.class", "ghIssues/issue582/package-info.class");
        assertBugAtLine("RV_RETURN_VALUE_IGNORED", 34);
    }
}
