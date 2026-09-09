package edu.umd.cs.findbugs.detect;


import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue2647Test extends AbstractIntegrationTest {
    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue2647.class");

        assertNoBugType("DMI_DOH");
    }
}
