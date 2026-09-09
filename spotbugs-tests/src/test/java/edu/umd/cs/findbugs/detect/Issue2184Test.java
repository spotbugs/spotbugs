package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue2184Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("../java17/ghIssues/Issue2184.class");
        assertBugInMethodAtLine("PT_RELATIVE_PATH_TRAVERSAL", "Issue2184", "test", 17);
    }
}
