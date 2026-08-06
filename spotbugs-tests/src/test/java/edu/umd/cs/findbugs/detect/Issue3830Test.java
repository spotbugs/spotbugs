package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/3830">GitHub issue #3830</a>
 */
class Issue3830Test extends AbstractIntegrationTest {
    @Test
    void testIssue3830() {
        performAnalysis("ghIssues/Issue3830.class", "ghIssues/Issue3830$Pair.class");
        assertNoBugType("DMI_RANDOM_USED_ONLY_ONCE");
    }
}
