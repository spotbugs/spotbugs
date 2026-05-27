package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3975Test extends AbstractIntegrationTest {

    @Test
    void testSaFieldDoubleAssignmentForWidePrimitives() {
        performAnalysis("ghIssues/Issue3975.class",
                "ghIssues/Issue3975$IntDoubleAssignment.class",
                "ghIssues/Issue3975$LongDoubleAssignment.class",
                "ghIssues/Issue3975$DoubleDoubleAssignment.class");

        assertBugInMethod("SA_FIELD_DOUBLE_ASSIGNMENT", "ghIssues.Issue3975$IntDoubleAssignment", "<init>");
        assertBugInMethod("SA_FIELD_DOUBLE_ASSIGNMENT", "ghIssues.Issue3975$LongDoubleAssignment", "<init>");
        assertBugInMethod("SA_FIELD_DOUBLE_ASSIGNMENT", "ghIssues.Issue3975$DoubleDoubleAssignment", "<init>");
    }
}
