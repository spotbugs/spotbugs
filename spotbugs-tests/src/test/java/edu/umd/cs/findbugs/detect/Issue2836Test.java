package edu.umd.cs.findbugs.detect;


import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue2836Test extends AbstractIntegrationTest {
    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue2836.class");

        assertNoBugType("NP_LOAD_OF_KNOWN_NULL_VALUE");
        assertNoBugType("RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE");
    }
}
