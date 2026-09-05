package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue1988Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue1988.class");

        assertBugTypeCount("UPM_UNCALLED_PRIVATE_METHOD", 0);
    }
}
