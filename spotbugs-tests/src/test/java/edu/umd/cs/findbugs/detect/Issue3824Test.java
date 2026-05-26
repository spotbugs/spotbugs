package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3824Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue3824.class", "ghIssues/Issue3824$DirectAssignment.class");

        assertBugInMethodCount("EI_EXPOSE_REP2", "ghIssues.Issue3824", "<init>", 1);
        assertBugInMethodCount("EI_EXPOSE_REP2", "ghIssues.Issue3824$DirectAssignment", "<init>", 1);
    }
}
