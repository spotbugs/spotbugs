package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue4295Test extends AbstractIntegrationTest {

    @Test
    void testRangeArrayIndexInNullConditions() {
        performAnalysis("ghIssues/Issue4295.class");

        // Exactly one RANGE_ARRAY_INDEX bug: only the reachable null-check body
        assertBugTypeCount("RANGE_ARRAY_INDEX", 1);

        // The impossible condition (value == null && value != null) is dead code - no bug
        assertNoBugInMethod("RANGE_ARRAY_INDEX", "Issue4295", "impossibleNullCondition");

        // The live null-check body is reachable - bug expected
        assertBugInMethod("RANGE_ARRAY_INDEX", "Issue4295", "reachableNullCheckBody");

        // Explicit null variable makes the body unreachable - no bug
        assertNoBugInMethod("RANGE_ARRAY_INDEX", "Issue4295", "explicitNullNoAccess");
    }
}
