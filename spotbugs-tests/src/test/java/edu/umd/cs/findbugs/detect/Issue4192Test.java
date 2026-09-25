package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue4192Test extends AbstractIntegrationTest {

    private static final String BUG = "INT_BAD_COMPARISON_WITH_SIGNED_BYTE";
    private static final String CLASS = "ghIssues.Issue4192";

    @Test
    void testSignedByteComparison() {
        performAnalysis("ghIssues/Issue4192.class");

        // Only always-true, never-true, out-of-range comparisons are reported.
        assertBugTypeCount(BUG, 5);
        assertBugInMethod(BUG, CLASS, "lessOrEqual127");
        assertBugInMethod(BUG, CLASS, "greaterThan127");
        assertBugInMethod(BUG, CLASS, "equal255");

        // Out-of-range comparisons must still be reported (no false negatives).
        assertBugInMethod(BUG, CLASS, "lessThan128");
        assertBugInMethod(BUG, CLASS, "greaterThanMinus129");

        // Meaningful comparisons against 127 must not be reported (the #4192 fix).
        assertNoBugInMethod(BUG, CLASS, "lessThan127");
        assertNoBugInMethod(BUG, CLASS, "greaterOrEqual127");
        assertNoBugInMethod(BUG, CLASS, "equal127");
    }

    @Test
    void testBadThingsToDoWithSignedBytes() {
        performAnalysis("BadThingsToDoWithSignedBytes.class");

        assertBugTypeCount(BUG, 16);
        assertBugInMethod(BUG, "BadThingsToDoWithSignedBytes", "compareGT127");
        assertBugInMethod(BUG, "BadThingsToDoWithSignedBytes", "compareLE127");
        // PR #4201 made compareGE127 and compareLT127 acceptable usages based on Issue #4192

        assertBugInMethod(BUG, "BadThingsToDoWithSignedBytes", "compareGT128");
        assertBugInMethod(BUG, "BadThingsToDoWithSignedBytes", "compareGE128");
        assertBugInMethod(BUG, "BadThingsToDoWithSignedBytes", "compareNE128");
        assertBugInMethod(BUG, "BadThingsToDoWithSignedBytes", "compareEQ128");
        assertBugInMethod(BUG, "BadThingsToDoWithSignedBytes", "compareLE128");
        assertBugInMethod(BUG, "BadThingsToDoWithSignedBytes", "compareLT128");

        assertBugInMethod(BUG, "BadThingsToDoWithSignedBytes", "compareGT200");
        assertBugInMethod(BUG, "BadThingsToDoWithSignedBytes", "compareGE200");
        assertBugInMethod(BUG, "BadThingsToDoWithSignedBytes", "compareNE200");
        assertBugInMethod(BUG, "BadThingsToDoWithSignedBytes", "compareEQ200");
        assertBugInMethod(BUG, "BadThingsToDoWithSignedBytes", "compareLE200");
        assertBugInMethod(BUG, "BadThingsToDoWithSignedBytes", "compareLT200");

        assertBugInMethod(BUG, "BadThingsToDoWithSignedBytes", "isHundred");
        assertBugInMethod(BUG, "BadThingsToDoWithSignedBytes", "isHundred2");
    }

}
