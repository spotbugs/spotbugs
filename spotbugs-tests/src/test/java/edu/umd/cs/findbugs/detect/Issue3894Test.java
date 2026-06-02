package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3894Test extends AbstractIntegrationTest {

    @Test
    void testUcfUselessControlFlowNotMissedInCatchOrAfterTryCatch() {
        performAnalysis("ghIssues/Issue3894.class");

        // All three UCF bugs in the same method must each be reported (deduplication regression)
        assertBugInMethodCount("UCF_USELESS_CONTROL_FLOW", "ghIssues.Issue3894", "multipleCasesInMethod", 3);

        // Case 1: empty if at start of method - already detected before the fix
        assertBugInMethodAtLine("UCF_USELESS_CONTROL_FLOW", "ghIssues.Issue3894", "multipleCasesInMethod", 25);

        // Case 2: empty if inside catch block - false negative now fixed
        assertBugInMethodAtLine("UCF_USELESS_CONTROL_FLOW", "ghIssues.Issue3894", "multipleCasesInMethod", 30);

        // Case 3: empty if after try-catch block - false negative now fixed
        assertBugInMethodAtLine("UCF_USELESS_CONTROL_FLOW", "ghIssues.Issue3894", "multipleCasesInMethod", 33);

        // Case 4: empty if with constant short-circuit - always detected (different bug type)
        assertBugInMethod("UCF_USELESS_CONTROL_FLOW_NEXT_LINE", "ghIssues.Issue3894", "multipleCasesInMethod");

        // Multiple consecutive empty ifs must each be reported (UCF_NEXT_LINE deduplication regression)
        assertBugInMethodCount("UCF_USELESS_CONTROL_FLOW_NEXT_LINE", "ghIssues.Issue3894", "multipleEmptyIfs", 3);

        // Non-empty if should NOT be reported
        assertNoBugInMethod("UCF_USELESS_CONTROL_FLOW", "ghIssues.Issue3894", "nonEmptyIf");
        assertNoBugInMethod("UCF_USELESS_CONTROL_FLOW_NEXT_LINE", "ghIssues.Issue3894", "nonEmptyIf");
    }
}
