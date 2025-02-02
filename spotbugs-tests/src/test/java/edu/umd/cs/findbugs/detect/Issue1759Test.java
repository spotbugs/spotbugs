package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue1759Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue1759.class");
        assertBugTypeCount("BC_IMPOSSIBLE_DOWNCAST_OF_TOARRAY", 1);
    }
}
