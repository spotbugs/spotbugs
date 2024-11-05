package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue2142Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue2142.class",
                "ghIssues/Issue2142$Inner.class");

        assertBugTypeCount("SA_FIELD_SELF_ASSIGNMENT", 2);
        assertBugInMethodAtLine("SA_FIELD_SELF_ASSIGNMENT", "Issue2142", "foo1", 6);
        assertBugInMethodAtLine("SA_FIELD_SELF_ASSIGNMENT", "Issue2142$Inner", "foo2", 10);
    }
}
