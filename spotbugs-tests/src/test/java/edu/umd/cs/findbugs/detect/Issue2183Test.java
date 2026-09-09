package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.annotations.Confidence;

class Issue2183Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("../java11/ghIssues/Issue2183.class");
        assertBugInMethodAtLineWithConfidence("SQL_NONCONSTANT_STRING_PASSED_TO_EXECUTE", "Issue2183", "test", 11, Confidence.HIGH);
    }
}
