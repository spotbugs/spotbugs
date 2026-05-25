package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue4007Test extends AbstractIntegrationTest {

    @Test
    void testByteBufferArrayAndArraysCopyOfExposure() {
        performAnalysis("ghIssues/Issue4007.class");

        assertBugInMethodAtField("EI_EXPOSE_BUF", "Issue4007", "getArray", "buf");
        assertBugInMethodAtField("MS_EXPOSE_BUF", "Issue4007", "getStaticArray", "S_BUF");
        assertBugInMethodAtField("EI_EXPOSE_BUF", "Issue4007", "getDuplicate", "buf");
        assertBugInMethodAtField("EI_EXPOSE_REP2", "Issue4007", "setDates", "dates");
        assertBugInMethodAtField("EI_EXPOSE_REP", "Issue4007", "getDatesClone", "dates");
    }
}
