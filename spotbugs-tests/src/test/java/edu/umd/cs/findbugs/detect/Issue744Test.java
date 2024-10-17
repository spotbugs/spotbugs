package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/744">GitHub issue #744</a>
 */
class Issue744Test extends AbstractIntegrationTest {
    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue744.class");
        assertBugTypeCount("DM_BOXED_PRIMITIVE_FOR_PARSING", 4);
    }
}
