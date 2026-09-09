package edu.umd.cs.findbugs.ba;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue371Test extends AbstractIntegrationTest {
    @Test
    void testIssue() {
        performAnalysis("lambdas/Issue371.class");
        assertBugTypeCount("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", 1);
    }
}
