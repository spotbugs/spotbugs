package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue1496Test extends AbstractIntegrationTest {

    @Test
    void reportsEqComparingClassNamesForGetNameEquals() {
        performAnalysis("ghIssues/Issue1496.class");
        assertBugInMethod("EQ_COMPARING_CLASS_NAMES", "ghIssues.Issue1496", "compareClassNames");
        assertBugInMethod("EQ_COMPARING_CLASS_NAMES", "ghIssues.Issue1496", "equalsStyle");
    }
}
