package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue4307Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("infiniteLoop/Issue4307.class");

        assertBugInMethod("IL_INFINITE_LOOP", "Issue4307", "testInfiniteFloatLoop");
        assertBugInMethod("IL_INFINITE_LOOP", "Issue4307", "testInfiniteDoubleLoop");
        assertBugTypeCount("IL_INFINITE_LOOP", 2);
    }
}
