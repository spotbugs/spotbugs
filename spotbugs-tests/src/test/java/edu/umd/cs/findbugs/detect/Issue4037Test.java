package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Regression test for
 * <a href="https://github.com/spotbugs/spotbugs/issues/4037">issue 4037</a>:
 * {@code NP_BOOLEAN_RETURN_NULL} must be reported when {@code null} is returned
 * from a {@code Boolean}-typed method via a local variable, not only when
 * written directly as {@code return null;}.
 */
class Issue4037Test extends AbstractIntegrationTest {

    private static final String BUG = "NP_BOOLEAN_RETURN_NULL";
    private static final String CLASS = "ghIssues.Issue4037";

    @Test
    void testNullReturnedViaLocalVariableIsReported() {
        performAnalysis("ghIssues/Issue4037.class");

        // Direct form: already detected before the fix.
        assertBugInMethod(BUG, CLASS, "isValidDirect");
        // Indirect form via a local variable: the case the fix targets.
        assertBugInMethod(BUG, CLASS, "isValidIndirect");
        assertBugInMethod(BUG, CLASS, "checkStatus");

        // Exactly three occurrences — the non-null return must not be flagged.
        assertBugTypeCount(BUG, 3);

        // Control: a method that never returns null must stay clean.
        assertNoBugInMethod(BUG, CLASS, "alwaysNonNull");
    }
}
