package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/1472">GitHub issue #1472</a>
 */
class Issue1472Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue1472.class");
        assertBugTypeCount("SA_LOCAL_SELF_COMPUTATION", 4);
    }
}
