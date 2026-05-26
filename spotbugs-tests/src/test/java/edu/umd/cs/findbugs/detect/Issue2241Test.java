package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue2241Test extends AbstractIntegrationTest {

    @Test
    void reportsFloatLoopCountersForAdditionalPatterns() {
        performAnalysis("ghIssues/Issue2241.class");
        assertBugInMethod("FL_FLOATS_AS_LOOP_COUNTERS", "ghIssues.Issue2241", "descendingFloatLoop");
        assertBugInMethod("FL_FLOATS_AS_LOOP_COUNTERS", "ghIssues.Issue2241", "incrementBeforeUse");
    }
}
