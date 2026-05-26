package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue3975Test extends AbstractIntegrationTest {

    @Test
    void reportsDoubleAssignmentForWidePrimitiveFields() {
        performAnalysis(
                "ghIssues/Issue3975$IntFieldDoubleAssignment.class",
                "ghIssues/Issue3975$LongFieldDoubleAssignment.class",
                "ghIssues/Issue3975$DoubleFieldDoubleAssignment.class");
        assertBugInMethod("SA_FIELD_DOUBLE_ASSIGNMENT", "ghIssues.Issue3975$IntFieldDoubleAssignment", "<init>");
        assertBugInMethod("SA_FIELD_DOUBLE_ASSIGNMENT", "ghIssues.Issue3975$LongFieldDoubleAssignment", "<init>");
        assertBugInMethod("SA_FIELD_DOUBLE_ASSIGNMENT", "ghIssues.Issue3975$DoubleFieldDoubleAssignment", "<init>");
    }
}
