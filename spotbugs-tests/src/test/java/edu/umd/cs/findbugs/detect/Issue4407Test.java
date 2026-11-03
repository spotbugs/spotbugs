package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue4407Test extends AbstractIntegrationTest {

    @Test
    void doesNotReportInfiniteLoopForRandomNormalLoop() {
        performAnalysis("ghIssues/Issue4407.class");
        assertBugTypeCount("IL_INFINITE_LOOP", 0);
    }

    @Test
    void stillReportsInfiniteLoopForFloatingPointComparisons() {
        performAnalysis("infiniteLoop/Issue4307.class");

        assertBugInMethod("IL_INFINITE_LOOP", "Issue4307", "testInfiniteFloatLoop");
        assertBugInMethod("IL_INFINITE_LOOP", "Issue4307", "testInfiniteDoubleLoop");
        assertBugTypeCount("IL_INFINITE_LOOP", 2);
    }
}
