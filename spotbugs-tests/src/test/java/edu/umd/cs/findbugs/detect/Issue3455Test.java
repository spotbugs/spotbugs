package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3455Test extends AbstractIntegrationTest {
    @Test
    void testNoDup() {
        performAnalysis("ghIssues/Issue3455.class");
        assertBugTypeCount("NP_GUARANTEED_DEREF", 1);
    }

    @Test
    void testWithDup() {
        performAnalysis("ghIssues/Issue3455.class");
        assertBugTypeCount("NP_GUARANTEED_DEREF", 1);
    }
}