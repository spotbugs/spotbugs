package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3310Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue3310.class",
                "ghIssues/Issue3310$Result.class");

        assertNoBugType("AT_STALE_THREAD_WRITE_OF_PRIMITIVE");
    }
}
