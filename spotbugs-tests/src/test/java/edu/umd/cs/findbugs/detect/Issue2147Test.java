package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue2147Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue2147.class",
                "ghIssues/Issue2147A.class",
                "ghIssues/Issue2147B.class",
                "ghIssues/Issue2147C.class");
        assertNoBugType("URF_UNREAD_FIELD");
    }
}
