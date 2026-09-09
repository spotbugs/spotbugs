package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/484">GitHub issue</a>
 * @since 3.1
 */
class Issue484Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue484.class");
        assertNoBugType("NP_NONNULL_PARAM_VIOLATION");
    }
}
