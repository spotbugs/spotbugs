package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue2837Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue2837.class");

        assertBugTypeCount("MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR", 1);
        assertBugAtLine("MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR", 19);
    }
}
