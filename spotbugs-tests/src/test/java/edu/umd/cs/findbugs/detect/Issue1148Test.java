package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue1148Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue1148.class",
                "ghIssues/Issue1148$UsageType.class");

        assertBugInMethodCount("SF_SWITCH_NO_DEFAULT", "ghIssues.Issue1148", "addData", 0);
    }
}
