package edu.umd.cs.findbugs.detect;


import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue3752Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue3752.class");

        assertBugInMethod("BC_IMPOSSIBLE_DOWNCAST_OF_TOARRAY", "ghIssues.Issue3752", "streamBadCast");
        assertBugInMethod("BC_IMPOSSIBLE_DOWNCAST_OF_TOARRAY", "ghIssues.Issue3752", "collectionBadCast");
    }
}
