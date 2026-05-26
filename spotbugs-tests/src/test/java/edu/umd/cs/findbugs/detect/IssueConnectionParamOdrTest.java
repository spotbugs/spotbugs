package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class IssueConnectionParamOdrTest extends AbstractIntegrationTest {

    @Test
    void reportsOpenDatabaseResourceForStatementFromConnectionParameter() {
        performAnalysis("ghIssues/IssueConnectionParamOdr.class");
        assertBugInMethod("ODR_OPEN_DATABASE_RESOURCE", "ghIssues.IssueConnectionParamOdr", "statementFromConnectionParameter");
    }
}
