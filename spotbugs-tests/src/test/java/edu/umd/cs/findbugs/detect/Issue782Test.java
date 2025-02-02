package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue782Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis(
                "ghIssues/Issue782.class",
                "ghIssues/Issue782$MyNullable.class");

        assertBugTypeCount("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", 1);

        assertBugAtLine("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", 27);
    }
}
