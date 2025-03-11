package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3351Test extends AbstractIntegrationTest {

    @Test
    void testSuppression() {
        performAnalysis("ghIssues/Issue3351.class",
                "ghIssues/Issue3351$Issue3351SuppressedNoSuchElement.class",
                "ghIssues/Issue3351$Issue3351NotSuppressedNoSuchElement.class");

        // Case of a suppressed bug: check that we don't report an useless suppression
        assertBugInClassCount("US_USELESS_SUPPRESSION_ON_METHOD", "ghIssues.Issue3351$Issue3351SuppressedNoSuchElement", 0);
        assertBugInClassCount("IT_NO_SUCH_ELEMENT", "ghIssues.Issue3351$Issue3351SuppressedNoSuchElement", 0);

        // Case of a true positive (not suppressed): check that we only report the bug
        assertBugInClassCount("US_USELESS_SUPPRESSION_ON_METHOD", "ghIssues.Issue3351$Issue3351NotSuppressedNoSuchElement", 0);
        assertBugInClassCount("IT_NO_SUCH_ELEMENT", "ghIssues.Issue3351$Issue3351NotSuppressedNoSuchElement", 1);
    }
}
