package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3396Test extends AbstractIntegrationTest {

    @Test
    void testSuppression() {
        performAnalysis("ghIssues/Issue3396.class");

        assertBugInClassCount("US_USELESS_SUPPRESSION_ON_METHOD", "ghIssues.Issue3396", 0);
    }
}
