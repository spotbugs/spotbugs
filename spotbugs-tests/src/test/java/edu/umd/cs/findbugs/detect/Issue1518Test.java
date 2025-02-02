package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/1518">GitHub issue #1518</a>
 */
class Issue1518Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue1518.class");
        assertBugTypeCount("RV_01_TO_INT", 3);
    }
}
