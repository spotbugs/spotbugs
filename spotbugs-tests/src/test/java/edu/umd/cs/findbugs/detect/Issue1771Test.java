package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue1771Test extends AbstractIntegrationTest {

    @Test
    @EnabledForJreRange(min = JRE.JAVA_11)
    void testIssue() {
        performAnalysis("../java11/ghIssues/Issue1771.class");
        assertNoBugType("EI_EXPOSE_REP");
    }
}
