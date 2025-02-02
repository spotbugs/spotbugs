package edu.umd.cs.findbugs.ba;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/1464">GitHub issue #1464</a>
 */

class Issue1464Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue1464.class");
        assertBugTypeCount("DMI_RANDOM_USED_ONLY_ONCE", 2);
    }
}
