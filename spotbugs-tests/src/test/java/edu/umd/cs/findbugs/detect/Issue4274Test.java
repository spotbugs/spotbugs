package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue4274Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("ghIssues/Issue4274.class", "ghIssues/Issue4274$OtherWriter.class");

        assertBugInMethod("OS_OPEN_STREAM", "Issue4274", "missingClose");
        assertBugInMethod("OS_OPEN_STREAM", "Issue4274", "closeOtherWriter");
        assertBugInMethod("OS_OPEN_STREAM", "Issue4274", "appendReturnsOtherWriter");
        assertBugInMethod("OS_OPEN_STREAM", "Issue4274", "upcastAppendReturnsOtherWriter");
        assertBugTypeCount("OS_OPEN_STREAM", 4);
        assertNoBugType("OS_OPEN_STREAM_EXCEPTION_PATH");
    }
}
