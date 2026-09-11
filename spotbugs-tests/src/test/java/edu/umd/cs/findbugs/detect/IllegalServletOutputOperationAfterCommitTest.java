package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class IllegalServletOutputOperationAfterCommitTest extends AbstractIntegrationTest {
    private static final String ISOOAC_BUG_TYPE = "ISOOAC_ILLEGAL_SERVLET_OUTPUT_OPERATION_AFTER_COMMIT";

    @Test
    void testIllegalServletOutputOperationAfterCommit() {
        performAnalysis("servletopaftercommit/IllegalServletOpAfterCommit.class", "servletopaftercommit/MyAuditResponse.class");

        assertBugTypeCount(ISOOAC_BUG_TYPE, 29);

        final String className = "IllegalServletOpAfterCommit";
        assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testNoncompliant1", 124);
        assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testNoncompliant2", 147);
        assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testViolatingSetContentTypeAfterSendError", 158);
        assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testViolatingRedirectAfterFlush", 167);
        assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testViolatingSendErrorAfterGetOutputStream", 175);
        assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testViolatingNestedFinallyDoubleFlush", 190);
        assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testViolatingFlushBufferThenHeader", 199);
        assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testViolatingSetContentLengthAfterClose", 210);
        assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testViolatingWrapperDoubleFlush", 220);
        assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testViolatingCustomTypes", 234);
        assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testViolatingStreamAsParameter", 243);
        assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testViolatingGenericWriterWithResponseContext", 252);
        for (int i = 262; i <= 277; i++) {
            assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testViolatingRelaxedMethodsForCoverage", i);
        }
        assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testViolatingGenericWriterWithCustomResponseContext", 285);
    }
}
