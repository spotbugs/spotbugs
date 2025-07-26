package edu.umd.cs.findbugs.detect;


import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue1928Test extends AbstractIntegrationTest {
    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue1928.class");

        assertBugInMethod("DMI_MISLEADING_SUBSTRING", "ghIssues.Issue1928", "builder");
        assertBugInMethod("DMI_MISLEADING_SUBSTRING", "ghIssues.Issue1928", "buffer");
        assertBugInMethod("DMI_USELESS_SUBSTRING", "ghIssues.Issue1928", "simpleString");
    }
}
