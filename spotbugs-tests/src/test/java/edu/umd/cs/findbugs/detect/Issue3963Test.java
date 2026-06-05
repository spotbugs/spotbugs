package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue3963Test extends AbstractIntegrationTest {

    @Test
    void testNoInfiniteLoopForIntDoubleComparison() {
        performAnalysis("ghIssues/Issue3963.class");

        assertBugInMethodCount("IL_INFINITE_LOOP", "ghIssues.Issue3963", "testLoop2", 0);
    }
}
