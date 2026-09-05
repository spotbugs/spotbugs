package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3886Test extends AbstractIntegrationTest {

    @Test
    void testYodaStyleOddCheck() {
        performAnalysis("ghIssues/Issue3886.class");

        assertBugTypeCount("IM_BAD_CHECK_FOR_ODD", 2);

        // Standard style (i % 2 == 1) should be detected
        assertBugInMethod("IM_BAD_CHECK_FOR_ODD", "ghIssues.Issue3886", "standardCheck");

        // Yoda style (1 == i % 2) should also be detected (was false negative)
        assertBugInMethod("IM_BAD_CHECK_FOR_ODD", "ghIssues.Issue3886", "yodaCheck");

        // Non-negative guard should suppress the warning
        assertNoBugInMethod("IM_BAD_CHECK_FOR_ODD", "ghIssues.Issue3886", "yodaCheckNonNeg");

        // Math.abs should suppress the warning
        assertNoBugInMethod("IM_BAD_CHECK_FOR_ODD", "ghIssues.Issue3886", "yodaCheckMathAbs");
    }
}
