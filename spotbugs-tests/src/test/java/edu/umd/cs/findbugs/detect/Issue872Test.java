package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue872Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue872.class",
                "ghIssues/Issue872$Value.class");

        assertBugTypeCount("RV_RETURN_VALUE_IGNORED", 1);
        assertBugTypeCount("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", 1);

        assertBugAtLine("RV_RETURN_VALUE_IGNORED", 12);
        assertBugAtLine("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", 18);
    }
}
