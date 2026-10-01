package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue4304Test extends AbstractIntegrationTest {
    private static final String BUG_TYPE = "OS_OPEN_STREAM";
    private static final String CLASS_NAME = "Issue4304";

    @Test
    void test() {
        performAnalysis("ghIssues/Issue4304.class");

        assertNoBugInMethod(BUG_TYPE, CLASS_NAME, "closeWrappedPrintStream");
        assertNoBugInMethod(BUG_TYPE, CLASS_NAME, "closeWrappedBufferedOutputStream");
        assertNoBugInMethod(BUG_TYPE, CLASS_NAME, "closeWrappedPrintWriter");
        assertNoBugInMethod(BUG_TYPE, CLASS_NAME, "closeInnermostOfTwoWrappers");
        assertNoBugInMethod(BUG_TYPE, CLASS_NAME, "closeWrappedInputStream");
        assertNoBugInMethod(BUG_TYPE, CLASS_NAME, "closeWrapper");

        assertBugInMethod(BUG_TYPE, CLASS_NAME, "closeNothing");
        assertBugInMethod(BUG_TYPE, CLASS_NAME, "closeUnrelatedStream");
        assertBugInMethod(BUG_TYPE, CLASS_NAME, "closeWrappedOnOnePathOnly");
    }
}
