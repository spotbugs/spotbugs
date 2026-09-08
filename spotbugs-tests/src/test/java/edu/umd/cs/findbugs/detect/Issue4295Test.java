package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue4295Test extends AbstractIntegrationTest {

    @Test
    void testImpossibleNullConditionIsNotReported() {
        performAnalysis("ghIssues/Issue4295.class");

        assertNoBugInMethod("RANGE_ARRAY_INDEX", "Issue4295", "impossibleNullCondition");
    }

    @Test
    void testReachableNullCheckBodyIsReported() {
        performAnalysis("ghIssues/Issue4295.class");

        assertBugInMethod("RANGE_ARRAY_INDEX", "Issue4295", "reachableNullCheckBody");
    }

    @Test
    void testExplicitNullBodyIsNotReported() {
        performAnalysis("ghIssues/Issue4295.class");

        assertNoBugInMethod("RANGE_ARRAY_INDEX", "Issue4295", "explicitNullNoAccess");
    }
}
