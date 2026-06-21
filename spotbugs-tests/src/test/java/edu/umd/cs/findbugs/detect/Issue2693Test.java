package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue2693Test extends AbstractIntegrationTest {

    @Test
    void testNullDerefInCatchBlock() {
        performAnalysis("ghIssues/Issue2693.class");
        assertBugInMethodAtLine("NP_ALWAYS_NULL_EXCEPTION", "ghIssues.Issue2693", "nullDerefInCatch", 10);
    }
}
