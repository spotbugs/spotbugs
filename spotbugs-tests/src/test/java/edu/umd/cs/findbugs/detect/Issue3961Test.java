package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3961Test extends AbstractIntegrationTest {

    @Test
    void testUninitializedReferenceArrayElementDereference() {
        performAnalysis("ghIssues/Issue3961.class");
        assertBugInMethodAtLine("NP_ALWAYS_NULL", "ghIssues.Issue3961", "reproduceRisk", 9);
    }
}
