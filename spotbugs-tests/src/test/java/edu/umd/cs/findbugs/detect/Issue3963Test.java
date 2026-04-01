package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3963Test extends AbstractIntegrationTest {

    @Test
    void testIlInfiniteLoopFalsePositiveForWideningNumericComparisons() {
        performAnalysis("ghIssues/Issue3963.class");

        // Exactly 3 IL_INFINITE_LOOP bugs expected (one per true positive method)
        assertBugTypeCount("IL_INFINITE_LOOP", 3);

        // False positives (now fixed): loop exits immediately because condition is statically false
        assertNoBugInMethod("IL_INFINITE_LOOP", "ghIssues.Issue3963", "testDoubleComparisonFP");
        assertNoBugInMethod("IL_INFINITE_LOOP", "ghIssues.Issue3963", "testFloatComparisonFP");
        assertNoBugInMethod("IL_INFINITE_LOOP", "ghIssues.Issue3963", "testLongComparisonFP");

        // True positives: condition is statically true, loop is genuinely infinite
        assertBugInMethod("IL_INFINITE_LOOP", "ghIssues.Issue3963", "testDoubleComparisonTP");
        assertBugInMethod("IL_INFINITE_LOOP", "ghIssues.Issue3963", "testFloatComparisonTP");
        assertBugInMethod("IL_INFINITE_LOOP", "ghIssues.Issue3963", "testLongComparisonTP");
    }
}
