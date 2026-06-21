package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue1517Test extends AbstractIntegrationTest {

    private static final String CLASS = "ghIssues.Issue1517";

    @Test
    void testIncrementInExpression() {
        performAnalysis("ghIssues/Issue1517.class");

        assertBugInMethodAtLine("DLS_OVERWRITTEN_INCREMENT", CLASS, "incrementInAdditionRight", 7);
        assertBugInMethodAtLine("DLS_OVERWRITTEN_INCREMENT", CLASS, "incrementInAdditionLeft", 12);
        assertBugInMethodAtLine("DLS_OVERWRITTEN_INCREMENT", CLASS, "decrementInSubtractionLeft", 17);
        assertBugInMethodAtLine("DLS_OVERWRITTEN_INCREMENT", CLASS, "decrementInAdditionRight", 22);
        assertBugInMethodAtLine("DLS_OVERWRITTEN_INCREMENT", CLASS, "directSelfAssignment", 27);
        assertBugInMethodCount("DLS_OVERWRITTEN_INCREMENT", CLASS, "standaloneIncrementThenAssign", 0);
    }
}
