package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue4428Test extends AbstractIntegrationTest {

    @Test
    void testDeadLocalStoreInLambda() {
        performAnalysis("ghIssues/Issue4428.class");
        assertBugTypeCount("DLS_DEAD_LOCAL_STORE", 2);
    }
}
