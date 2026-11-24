package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/4401">GitHub issue #4401</a>
 */
class Issue4401Test extends AbstractIntegrationTest {

    @Test
    void testShortCircuitNullCheckInConstructor() {
        performAnalysis(
                "ghIssues/Issue4401.class",
                "ghIssues/Issue4401$Update.class",
                "ghIssues/Issue4401$UpdateClass.class",
                "ghIssues/Issue4401$UpdateSwapped.class",
                "ghIssues/Issue4401$AlwaysNullField.class");

        assertNoBugInMethod("NP_NULL_INSTANCEOF", "Issue4401", "processUpdateDirectFieldAccess");
        assertNoBugInMethod("NP_NULL_INSTANCEOF", "Issue4401", "processUpdateClass");
        assertNoBugInMethod("NP_NULL_INSTANCEOF", "Issue4401", "processUpdateClassExplicitNullCheck");
        assertNoBugInMethod("NP_NULL_INSTANCEOF", "Issue4401", "processUpdateSwapped");

        assertBugInMethod("NP_NULL_INSTANCEOF", "Issue4401", "processAlwaysNullField");
        assertBugTypeCount("NP_NULL_INSTANCEOF", 1);
    }
}
