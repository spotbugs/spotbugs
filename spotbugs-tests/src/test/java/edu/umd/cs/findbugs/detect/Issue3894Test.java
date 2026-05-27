package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3894Test extends AbstractIntegrationTest {

    @Test
    void reportsEachUselessControlFlowOccurrenceInMethod() {
        performAnalysis("ghIssues/Issue3894.class");

        assertBugInMethodCount("UCF_USELESS_CONTROL_FLOW", "ghIssues.Issue3894", "multipleCasesInMethod", 4);
        assertBugInMethodAtLine("UCF_USELESS_CONTROL_FLOW", "ghIssues.Issue3894", "multipleCasesInMethod", 13);
        assertBugInMethodAtLine("UCF_USELESS_CONTROL_FLOW", "ghIssues.Issue3894", "multipleCasesInMethod", 19);
        assertBugInMethodAtLine("UCF_USELESS_CONTROL_FLOW", "ghIssues.Issue3894", "multipleCasesInMethod", 23);
        assertBugInMethodAtLine("UCF_USELESS_CONTROL_FLOW", "ghIssues.Issue3894", "multipleCasesInMethod", 26);

        assertBugInMethodCount("UCF_USELESS_CONTROL_FLOW", "ghIssues.Issue3894", "multipleEmptyIfs", 3);
        assertBugInMethodAtLine("UCF_USELESS_CONTROL_FLOW", "ghIssues.Issue3894", "multipleEmptyIfs", 31);
        assertBugInMethodAtLine("UCF_USELESS_CONTROL_FLOW", "ghIssues.Issue3894", "multipleEmptyIfs", 33);
        assertBugInMethodAtLine("UCF_USELESS_CONTROL_FLOW", "ghIssues.Issue3894", "multipleEmptyIfs", 35);

        assertNoBugInMethod("UCF_USELESS_CONTROL_FLOW", "ghIssues.Issue3894", "nonEmptyIf");
    }
}
