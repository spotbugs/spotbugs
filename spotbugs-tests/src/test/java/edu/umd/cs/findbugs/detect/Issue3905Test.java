package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3905Test extends AbstractIntegrationTest {

    @Test
    void testSwitchNoDefaultWithEmptyLastCase() {
        performAnalysis("ghIssues/Issue3905.class");

        assertBugInMethod("SF_SWITCH_NO_DEFAULT", "ghIssues.Issue3905", "testWithAssignmentInLastCase");
        assertBugInMethod("SF_SWITCH_NO_DEFAULT", "ghIssues.Issue3905", "testWithBreakOnlyInLastCase");
    }
}
