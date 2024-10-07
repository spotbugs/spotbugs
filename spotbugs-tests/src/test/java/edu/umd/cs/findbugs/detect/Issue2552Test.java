package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue2552Test extends AbstractIntegrationTest {

    @Test
    @DisabledOnJre({ JRE.JAVA_8, JRE.JAVA_11 })
    void testIssue() {
        performAnalysis("../java17/ghIssues/Issue2552.class");

        assertBugTypeCount("EI_EXPOSE_REP", 1);

        assertBugAtLine("EI_EXPOSE_REP", 12);
    }
}
