package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3955Test extends AbstractIntegrationTest {

    @Test
    void testUnreadFieldInEnum() {
        performAnalysis("ghIssues/Issue3955.class",
                "ghIssues/Issue3955$Inner.class");

        assertBugAtField("URF_UNREAD_FIELD", "ghIssues.Issue3955$Inner", "innerField");
        assertBugAtField("URF_UNREAD_FIELD", "ghIssues.Issue3955", "enumField");
    }
}
