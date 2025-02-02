package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue1765Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue1765.class");
        assertBugTypeCount("HE_HASHCODE_USE_OBJECT_EQUALS", 1);
    }
}
