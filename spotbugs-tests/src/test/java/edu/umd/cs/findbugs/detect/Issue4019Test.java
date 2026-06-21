package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue4019Test extends AbstractIntegrationTest {

    @Test
    void testStatementSupertypeResourceLeak() {
        performAnalysis("ghIssues/Issue4019.class");

        assertBugInMethodCount("ODR_OPEN_DATABASE_RESOURCE", "ghIssues.Issue4019", "fetchUserBefore", 1);
        assertBugInMethodCount("ODR_OPEN_DATABASE_RESOURCE", "ghIssues.Issue4019", "fetchUserAfter", 1);
    }
}
