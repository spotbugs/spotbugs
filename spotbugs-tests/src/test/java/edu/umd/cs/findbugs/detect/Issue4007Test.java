package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue4007Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue4007.class");

        assertBugTypeCount("EI_EXPOSE_BUF", 1);
        assertBugTypeCount("MS_EXPOSE_BUF", 1);
        assertBugTypeCount("EI_EXPOSE_REP2", 1);
        assertBugTypeCount("EI_EXPOSE_BUF2", 0);

        assertBugInMethodAtField("EI_EXPOSE_BUF", "Issue4007", "getArray", "buf");
        assertBugInMethodAtField("MS_EXPOSE_BUF", "Issue4007", "getStaticArray", "S_BUF");
        assertBugInMethodAtField("EI_EXPOSE_REP2", "Issue4007", "setBacking", "backing");
    }
}
