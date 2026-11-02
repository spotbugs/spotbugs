package ghIssues;

/**
 * Test cases for GitHub issue #3894:
 * UCF_USELESS_CONTROL_FLOW false negatives in catch blocks and before terminal statements.
 *
 * Root cause: BugAccumulator deduplicated multiple UCF bugs in the same method when
 * MERGE_SIMILAR_WARNINGS=true (the default). The fix replaces BugAccumulator with direct
 * bugReporter.reportBug() calls so each occurrence is reported independently.
 */
public class Issue3894 {

    static int x = 0;
    static int y = 0;

    static void trigger() throws Exception {
        throw new Exception();
    }

    // Case 1 (line 25): empty if at start of method - always detected
    // Case 2 (line 30): empty if inside catch block - was deduplicated (FN), now fixed
    // Case 3 (line 33): empty if after try-catch - was deduplicated (FN), now fixed
    // Case 4 (line 35): empty if with constant short-circuit - always detected (different bug type)
    public static void multipleCasesInMethod() {
        if (x > 0) {}

        try {
            trigger();
        } catch (Exception e) {
            if (x == 0) {}
        }

        if (y != 0) {}

        if (x == 0 && true) {}
    }

    // Regression: multiple empty ifs on consecutive lines in the same method must all be reported
    public static void multipleEmptyIfs(int a, int b, int c) {
        if (a > 0) {}
        if (b > 0) {}
        if (c > 0) {}
    }

    // Should NOT report: non-empty if body
    public static void nonEmptyIf() {
        if (x > 0) {
            y = 1;
        }
    }
}
