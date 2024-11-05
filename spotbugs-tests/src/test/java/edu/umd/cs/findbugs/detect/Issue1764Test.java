package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/1764">GitHub issue</a>
 */
class Issue1764Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        System.setProperty("frc.debug", "true");
        performAnalysis("ghIssues/Issue1764.class");
        assertBugAtLine("ES_COMPARING_STRINGS_WITH_EQ", 7);
    }
}
