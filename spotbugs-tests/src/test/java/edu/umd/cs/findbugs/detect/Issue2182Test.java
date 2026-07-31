package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue2182Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("../java11/ghIssues/Issue2182.class");
        assertBugInMethodAtLine("SBSC_USE_STRINGBUFFER_CONCATENATION", "Issue2182", "test", 22);
    }
}
