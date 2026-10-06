package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue4147Test extends AbstractIntegrationTest {

    @Test
    void testOperandOrderDoesNotChangeNullVerdict() {
        performAnalysis("ghIssues/Issue4147.class");

        assertBugTypeCount("NP_NULL_ON_SOME_PATH", 1);
        assertNoBugInMethod("NP_NULL_ON_SOME_PATH", "Issue4147", "directForm");
        assertNoBugInMethod("NP_NULL_ON_SOME_PATH", "Issue4147", "yodaForm");
        assertBugInMethodCount("NP_NULL_ON_SOME_PATH", "Issue4147", "dereferencedWhenNull", 1);
    }
}
