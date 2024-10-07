package edu.umd.cs.findbugs.ba;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue389Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue389.class");
        assertBugTypeCount("UC_USELESS_VOID_METHOD", 3);
        assertBugTypeCount("DLS_DEAD_LOCAL_STORE", 3);
    }
}
