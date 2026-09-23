package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Unit test for <a href="https://github.com/spotbugs/spotbugs/issues/4309">#4309</a>.
 */
class Issue4309Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue4309.class");

        assertBugTypeCount("MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR", 1);
        assertBugAtLine("MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR", 13);
    }
}
