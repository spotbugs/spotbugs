package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/3975">GitHub issue</a>
 */
class Issue3975Test extends AbstractIntegrationTest {

    @Test
    void testDoubleAssignmentOfWideFields() {
        performAnalysis("ghIssues/Issue3975.class");

        assertBugTypeCount("SA_FIELD_DOUBLE_ASSIGNMENT", 3);
        assertBugAtField("SA_FIELD_DOUBLE_ASSIGNMENT", "ghIssues.Issue3975", "intValue");
        assertBugAtField("SA_FIELD_DOUBLE_ASSIGNMENT", "ghIssues.Issue3975", "longValue");
        assertBugAtField("SA_FIELD_DOUBLE_ASSIGNMENT", "ghIssues.Issue3975", "doubleValue");
        assertNoBugAtField("SA_FIELD_DOUBLE_ASSIGNMENT", "ghIssues.Issue3975", "readBetween");
    }
}
