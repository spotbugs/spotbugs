package edu.umd.cs.findbugs.detect;


import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue1928Test extends AbstractIntegrationTest {
    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue1928.class");

        // StringBuilder
        assertBugInMethodCount("DMI_MISLEADING_SUBSTRING", "ghIssues.Issue1928", "builder", 1);
        assertNoBugInMethod("DMI_USELESS_SUBSTRING", "ghIssues.Issue1928", "builder");

        // StringBuffer
        assertBugInMethodCount("DMI_MISLEADING_SUBSTRING", "ghIssues.Issue1928", "buffer", 1);
        assertNoBugInMethod("DMI_USELESS_SUBSTRING", "ghIssues.Issue1928", "buffer");

        // String
        assertBugInMethodCount("DMI_USELESS_SUBSTRING", "ghIssues.Issue1928", "simpleString", 1);
        assertNoBugInMethod("DMI_MISLEADING_SUBSTRING", "ghIssues.Issue1928", "simpleString");
    }
}
