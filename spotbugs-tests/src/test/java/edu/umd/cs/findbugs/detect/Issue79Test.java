package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


/**
 * SpotBugs should remove the ResultSet obligation from all set
 * when one occurrence of that type of obligation is Statement
 * from all states.
 *
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/79">GitHub issue</a>
 * @since 4.1.3
 */
class Issue79Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue79.class");
        assertNoBugType("OBL_UNSATIFIED_OBLIGATION");
    }
}
