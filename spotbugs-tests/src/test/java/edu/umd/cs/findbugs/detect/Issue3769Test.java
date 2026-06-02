package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3769Test extends AbstractIntegrationTest {

    @Test
    void testImpossibleDowncastAfterClone() {
        performAnalysis("ghIssues/Issue3769.class");
        assertBugInMethod("BC_IMPOSSIBLE_DOWNCAST", "ghIssues.Issue3769", "main");
    }
}
