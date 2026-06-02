package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3985Test extends AbstractIntegrationTest {

    @Test
    void testNpNullOnSomePathWithShortCircuitAndNullCheck() {
        performAnalysis("ghIssues/Issue3985.class");

        assertBugInMethod("NP_NULL_ON_SOME_PATH", "ghIssues.Issue3985", "reproduceFN");
        assertBugInMethod("NP_NULL_ON_SOME_PATH", "ghIssues.Issue3985", "reproduceFP");
    }
}
