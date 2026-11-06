package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue4430Test extends AbstractIntegrationTest {

    @Test
    void reportsVacuousInstanceofInLambda() {
        performAnalysis("ghIssues/Issue4430.class");

        assertBugTypeCount("BC_VACUOUS_INSTANCEOF", 1);
        assertBugInMethodAtLine("BC_VACUOUS_INSTANCEOF", "Issue4430", "lambda$run$0", 7);
    }
}
