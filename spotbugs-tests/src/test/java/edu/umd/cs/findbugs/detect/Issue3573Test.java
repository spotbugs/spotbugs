package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

import org.junit.jupiter.api.Test;

class Issue3573Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue3573.class",
                "ghIssues/Issue3573$HashBiDictionnary.class");

        assertBugTypeCount("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", 0);
    }
}
