package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3955Test extends AbstractIntegrationTest {

    @Test
    void testUnreadFieldInEnum() {
        performAnalysis("ghIssues/A.class",
                "ghIssues/A$B.class");

        assertBugTypeCount("URF_UNREAD_FIELD", 2);
        assertBugAtField("URF_UNREAD_FIELD", "ghIssues.A$B", "s");
        assertBugAtField("URF_UNREAD_FIELD", "ghIssues.A", "i");
    }
}
