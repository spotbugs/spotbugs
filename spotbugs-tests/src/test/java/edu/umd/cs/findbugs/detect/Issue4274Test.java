package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue4274Test extends AbstractIntegrationTest {

    @ParameterizedTest
    @ValueSource(strings = { "appendCharSequence", "appendChar", "appendSubsequence", "appendChain", "closeAlias" })
    void appendPreservesClosedWriter(String method) {
        performAnalysis("ghIssues/Issue4274.class", "ghIssues/Issue4274$OtherWriter.class");

        assertNoBugInMethod("OS_OPEN_STREAM", "Issue4274", method);
        assertNoBugInMethod("OS_OPEN_STREAM_EXCEPTION_PATH", "Issue4274", method);
    }

    @ParameterizedTest
    @ValueSource(strings = { "missingClose", "closeOtherWriter", "appendReturnsOtherWriter", "upcastAppendReturnsOtherWriter" })
    void stillReportsUnclosedWriter(String method) {
        performAnalysis("ghIssues/Issue4274.class", "ghIssues/Issue4274$OtherWriter.class");

        assertBugInMethod("OS_OPEN_STREAM", "Issue4274", method);
    }
}
