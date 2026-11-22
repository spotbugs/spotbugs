package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/2128">GitHub issue #2128</a>
 */
class Issue2128Test extends AbstractIntegrationTest {
    @Test
    void testLazyStaticRandomIsNotReported() {
        performAnalysis("ghIssues/Issue2128.class");
        assertNoBugType("DMI_RANDOM_USED_ONLY_ONCE");
    }
}
