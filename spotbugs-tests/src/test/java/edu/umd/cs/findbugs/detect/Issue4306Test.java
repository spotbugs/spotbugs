package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue4306Test extends AbstractIntegrationTest {

    @Test
    void testDmUselessThreadForThreadSubclass() {
        performAnalysis("ghIssues/Issue4306.class",
                "ghIssues/Issue4306$Subject.class",
                "ghIssues/Issue4306$SubjectWithRun.class");
        assertBugInMethod("DM_USELESS_THREAD", "ghIssues.Issue4306", "test");
    }

    @Test
    void testNoDmUselessThreadForThreadSubclassWithRun() {
        performAnalysis("ghIssues/Issue4306.class",
                "ghIssues/Issue4306$Subject.class",
                "ghIssues/Issue4306$SubjectWithRun.class");
        assertNoBugInMethod("DM_USELESS_THREAD", "ghIssues.Issue4306", "testThreadSubclassWithRun");
    }
}
