package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue3986Test extends AbstractIntegrationTest {

    @Test
    void testInconsistentSyncWithoutNullCheck() {
        performAnalysis("ghIssues/Issue3986.class");

        assertBugTypeCount("IS2_INCONSISTENT_SYNC", 1);
    }
}
