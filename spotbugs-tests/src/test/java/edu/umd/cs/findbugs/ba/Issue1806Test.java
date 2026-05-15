package edu.umd.cs.findbugs.ba;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/1806">GitHub issue #1806</a>
 */
class Issue1806Test extends AbstractIntegrationTest {
    @Test
    void testIssue1806() {
        performAnalysis("ghIssues/Issue1806.class");
        assertNoBugType("DMI_RANDOM_USED_ONLY_ONCE");
    }
}
