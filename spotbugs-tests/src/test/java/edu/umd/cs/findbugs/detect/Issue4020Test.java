package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue4020Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue4020.class");
        assertBugTypeCount("OBL_UNSATISFIED_OBLIGATION", 1);
    }
}
