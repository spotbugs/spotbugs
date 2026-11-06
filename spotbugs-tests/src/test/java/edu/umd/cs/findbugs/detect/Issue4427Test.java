package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/4427">GitHub issue</a>
 */
class Issue4427Test extends AbstractIntegrationTest {

    @Test
    void testCopyInLoopKeepsNullOnSomePath() {
        performAnalysis("ghIssues/Issue4427.class");

        assertBugInMethodAtLine("NP_NULL_ON_SOME_PATH", "Issue4427", "copyInLoop", 14);
        assertBugInMethodAtLine("NP_NULL_ON_SOME_PATH", "Issue4427", "noCopyInLoop", 26);
        assertBugTypeCount("NP_NULL_ON_SOME_PATH", 2);
    }
}
