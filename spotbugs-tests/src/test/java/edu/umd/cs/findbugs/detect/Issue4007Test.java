package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue4007Test extends AbstractIntegrationTest {

    @Test
    void testByteBufferArrayExposure() {
        performAnalysis("ghIssues/Issue4007.class");

        assertBugInMethod("EI_EXPOSE_BUF", "ghIssues.Issue4007", "getArray");
        assertBugInMethod("MS_EXPOSE_BUF", "ghIssues.Issue4007", "getStaticArray");
        assertBugInMethod("EI_EXPOSE_BUF", "ghIssues.Issue4007", "getDuplicate");
        assertBugInMethod("EI_EXPOSE_REP2", "ghIssues.Issue4007", "setDates");
        assertBugInMethod("EI_EXPOSE_REP", "ghIssues.Issue4007", "getDatesClone");
        assertNoBugInMethod("EI_EXPOSE_BUF", "ghIssues.Issue4007", "getArrayCopy");
    }
}
