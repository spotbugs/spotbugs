package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue1293Test extends AbstractIntegrationTest {

    @Test
    void reportsOdrWhenResourcesObtainedViaHelperMethods() {
        performAnalysis("ghIssues/Issue1293.class");
        assertBugInMethod("ODR_OPEN_DATABASE_RESOURCE", "ghIssues.Issue1293", "getList");
    }
}
