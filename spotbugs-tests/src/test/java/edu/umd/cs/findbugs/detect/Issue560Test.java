package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue560Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue560.class",
                "ghIssues/Issue560$NotANestedTest.class",
                "ghIssues/Issue560$NestedTest.class");

        assertBugTypeCount("SIC_INNER_SHOULD_BE_STATIC", 1);
        assertBugInClass("SIC_INNER_SHOULD_BE_STATIC", "ghIssues.Issue560$NotANestedTest");
    }
}
