package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue2182Test extends AbstractIntegrationTest {

    @Test
    @EnabledForJreRange(min = JRE.JAVA_11)
    void testIssue() {
        performAnalysis("../java11/ghIssues/Issue2182.class");
        assertBugInMethodAtLine("SBSC_USE_STRINGBUFFER_CONCATENATION", "Issue2182", "test", 22);
    }
}
