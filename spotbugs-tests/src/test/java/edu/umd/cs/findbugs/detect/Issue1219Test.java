package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue1219Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue1219.class",
                "ghIssues/Issue1219$Parent.class",
                "ghIssues/Issue1219$Child.class",
                "ghIssues/Issue1219$Factory.class",
                "ghIssues/Issue1219$Test.class");

        assertBugTypeCount("BC_UNCONFIRMED_CAST_OF_RETURN_VALUE", 1);
        assertBugAtLine("BC_UNCONFIRMED_CAST_OF_RETURN_VALUE", 36);
    }
}
