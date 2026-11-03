package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue4438Test extends AbstractIntegrationTest {

    @Test
    void testIlInfiniteLoopForReversedComparisonOperands() {
        performAnalysis("ghIssues/Issue4438.class");

        // `while (s.trim().length() == 0)` with s = "foo": javac emits a single-operand `ifne`.
        assertBugInMethod("IL_INFINITE_LOOP", "ghIssues.Issue4438", "infiniteLoopPlainOperandOrder");

        // `while (0 == s.trim().length())` with s = "foo": the same condition, but javac emits
        // `iconst_0` followed by a two-operand `if_icmpne`. It must be reported as well.
        assertBugInMethod("IL_INFINITE_LOOP", "ghIssues.Issue4438", "infiniteLoopReversedOperandOrder");

        assertBugTypeCount("IL_INFINITE_LOOP", 2);
    }
}
