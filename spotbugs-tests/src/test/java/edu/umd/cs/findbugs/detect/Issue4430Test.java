package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue4430Test extends AbstractIntegrationTest {

    @Test
    void reportsVacuousInstanceofInLambdaBody() {
        performAnalysis("ghIssues/Issue4430.class",
                "ghIssues/Issue4430$1.class");

        assertBugTypeCount("BC_VACUOUS_INSTANCEOF", 3);
        assertBugInMethod("BC_VACUOUS_INSTANCEOF", "Issue4430", "normalMethod");
        assertBugInMethod("BC_VACUOUS_INSTANCEOF", "Issue4430", "lambda$lambdaMethod$0");
        assertBugInMethod("BC_VACUOUS_INSTANCEOF", "Issue4430$1", "run");
    }
}
