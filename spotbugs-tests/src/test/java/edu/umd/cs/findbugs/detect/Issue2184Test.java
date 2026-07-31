package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue2184Test extends AbstractIntegrationTest {

    @Test
    @EnabledForJreRange(min = JRE.JAVA_17)
    void testIssue() {
        performAnalysis("../java17/ghIssues/Issue2184.class");
        assertBugInMethodAtLine("PT_RELATIVE_PATH_TRAVERSAL", "Issue2184", "test", 17);
    }
}
