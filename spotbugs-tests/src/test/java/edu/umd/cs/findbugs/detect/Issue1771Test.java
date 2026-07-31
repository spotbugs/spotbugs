package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue1771Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("../java11/ghIssues/Issue1771.class");
        assertNoBugType("EI_EXPOSE_REP");
    }
}
