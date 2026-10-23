
package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue4407Test extends AbstractIntegrationTest {

    @Test
    void testFiniteLoopsAreNotReportedAsInfinite() {
        performAnalysis("ghIssues/Issue4407.class");

        assertNoBugInMethod(
                "IL_INFINITE_LOOP",
                "ghIssues.Issue4407",
                "main");
    }
}
