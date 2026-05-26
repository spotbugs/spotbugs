package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue4019Test extends AbstractIntegrationTest {

    @Test
    void reportsOpenDatabaseResourceForStatementSupertype() {
        performAnalysis("ghIssues/Issue4019.class");
        assertBugInMethod("ODR_OPEN_DATABASE_RESOURCE", "ghIssues.Issue4019", "preparedStatementVariable");
        assertBugInMethod("ODR_OPEN_DATABASE_RESOURCE", "ghIssues.Issue4019", "statementSupertypeVariable");
    }
}
