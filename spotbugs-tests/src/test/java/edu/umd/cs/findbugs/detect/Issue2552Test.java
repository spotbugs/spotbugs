package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue2552Test extends AbstractIntegrationTest {

    @Test
    @EnabledForJreRange(min = JRE.JAVA_17)
    void testIssue() {
        performAnalysis("../java17/ghIssues/Issue2552.class");

        assertBugTypeCount("EI_EXPOSE_REP", 1);

        assertBugAtLine("EI_EXPOSE_REP", 12);
    }
}
