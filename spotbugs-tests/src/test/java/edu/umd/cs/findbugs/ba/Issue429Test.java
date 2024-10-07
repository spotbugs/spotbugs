package edu.umd.cs.findbugs.ba;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Unit test to reproduce <a href="https://github.com/spotbugs/spotbugs/issues/429">#429</a>.
 */
class Issue429Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue429.class");
        assertNoBugType("RV_RETURN_VALUE_IGNORED");
    }
}
