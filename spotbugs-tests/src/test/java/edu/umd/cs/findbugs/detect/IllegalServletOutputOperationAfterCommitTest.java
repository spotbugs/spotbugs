package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

public class IllegalServletOutputOperationAfterCommitTest extends AbstractIntegrationTest {
    private static final String ISOOAC_BUG_TYPE = "ISOOAC_ILLEGAL_SERVLET_OUTPUT_OPERATION_AFTER_COMMIT";

    @Test
    void testIllegalServletOutputOperationAfterCommit() {
        performAnalysis("servletopaftercommit/IllegalServletOpAfterCommit.class", "servletopaftercommit/MyAuditResponse.class");

        assertBugTypeCount(ISOOAC_BUG_TYPE, 29);

        final String className = "IllegalServletOpAfterCommit";
        assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testNoncompliant1", 126);
        assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testNoncompliant2", 149);
        assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testViolatingSetContentTypeAfterSendError", 160);
        assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testViolatingRedirectAfterFlush", 169);
        assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testViolatingSendErrorAfterGetOutputStream", 177);
        assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testViolatingNestedFinallyDoubleFlush", 192);
        assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testViolatingFlushBufferThenHeader", 201);
        assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testViolatingSetContentLengthAfterClose", 212);
        assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testViolatingWrapperDoubleFlush", 222);
        assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testViolatingCustomTypes", 236);
        assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testViolatingStreamAsParameter", 245);
        assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testViolatingGenericWriterWithResponseContext", 254);
        for (int i = 264; i <= 279; i++) {
            assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testViolatingRelaxedMethodsForCoverage", i);
        }
        assertBugInMethodAtLine(ISOOAC_BUG_TYPE, className, "testViolatingGenericWriterWithCustomResponseContext", 287);
    }
}
