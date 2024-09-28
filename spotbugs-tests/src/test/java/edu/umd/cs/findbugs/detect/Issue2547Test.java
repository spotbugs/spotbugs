package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/2547">GitHub issue #2547</a>
 */
class Issue2547Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/issue2547/Issue2547.class",
                "ghIssues/issue2547/MyEx.class",
                "ghIssues/issue2547/ExceptionFactory.class");

        assertBugTypeCount("RV_EXCEPTION_NOT_THROWN", 2);
        assertBugInMethodAtLine("RV_EXCEPTION_NOT_THROWN", "Issue2547", "notThrowingExCtor", 15);
        assertBugInMethodAtLine("RV_EXCEPTION_NOT_THROWN", "Issue2547", "notThrowingExCtorCaller", 26);
    }
}
