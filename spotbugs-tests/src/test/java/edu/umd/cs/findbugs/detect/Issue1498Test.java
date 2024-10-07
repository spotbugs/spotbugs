package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;

class Issue1498Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        System.setProperty("frc.debug", "true");
        performAnalysis("ghIssues/Issue1498.class");
        assertBugTypeCount("IM_MULTIPLYING_RESULT_OF_IREM", 3);
    }
}
