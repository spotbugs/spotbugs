
package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Regression test for GitHub issue #4430.
 */
class Issue4430Test extends AbstractIntegrationTest {

    @Test
    void testVacuousInstanceofInLambda() {
        performAnalysis(
                "ghIssues/Issue4430.class",
                "ghIssues/Issue4430$1.class");

        assertBugTypeCount("BC_VACUOUS_INSTANCEOF", 3);

        // Normal method should report.
        assertBugInMethod(
                "BC_VACUOUS_INSTANCEOF", "Issue4430", "normalMethod");

        // Lambda body should report.
        assertBugInMethod(
                "BC_VACUOUS_INSTANCEOF", "Issue4430", "lambda$lambdaMethod$0");

        // Anonymous class should continue to report.
        assertBugInMethod(
                "BC_VACUOUS_INSTANCEOF", "Issue4430$1", "run");
    }
}
