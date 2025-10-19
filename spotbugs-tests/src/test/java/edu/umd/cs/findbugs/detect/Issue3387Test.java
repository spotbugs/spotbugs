package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3387Test extends AbstractIntegrationTest {

    @Test
    void testSuppression() {
        performAnalysis("ghIssues/Issue3387.class");

        assertBugInClassCount("FS_BAD_DATE_FORMAT_FLAG_COMBO", "ghIssues.Issue3387", 0);
    }
}
