package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3267Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue3267.class",
                "ghIssues/Issue3267BaseClass.class");

        assertBugTypeCount("THROWS_METHOD_THROWS_CLAUSE_BASIC_EXCEPTION", 2);
    }
}
