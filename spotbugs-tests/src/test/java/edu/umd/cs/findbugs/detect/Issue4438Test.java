package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue4438Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("infiniteLoop/Issue4438.class");

        assertBugInMethod("IL_INFINITE_LOOP", "Issue4438", "alwaysTrue");
        assertBugInMethod("IL_INFINITE_LOOP", "Issue4438", "alwaysTrueSwapped");
        assertBugTypeCount("IL_INFINITE_LOOP", 2);
    }
}
