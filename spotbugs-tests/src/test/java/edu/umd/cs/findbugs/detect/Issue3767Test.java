package edu.umd.cs.findbugs.detect;


import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue3767Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue3767.class");

        assertBugTypeCount("SF_SWITCH_FALLTHROUGH", 0);
        assertBugTypeCount("SF_DEAD_STORE_DUE_TO_SWITCH_FALLTHROUGH", 0);
    }
}
