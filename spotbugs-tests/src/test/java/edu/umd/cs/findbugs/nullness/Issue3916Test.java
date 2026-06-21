package edu.umd.cs.findbugs.nullness;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Unit test to reproduce <a href="https://github.com/spotbugs/spotbugs/issues/3916">#3916</a>.
 */
class Issue3916Test extends AbstractIntegrationTest {

    @Test
    void testRedundantInstanceofIsNullCheck() {
        performAnalysis("ghIssues/Issue3916.class");

        assertBugInMethod("NP_ALWAYS_NULL", "ghIssues.Issue3916", "foo1");
        assertBugInMethod("NP_ALWAYS_NULL", "ghIssues.Issue3916", "foo2");
    }
}
