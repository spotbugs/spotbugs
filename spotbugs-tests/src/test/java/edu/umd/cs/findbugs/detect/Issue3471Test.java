package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue3471Test extends AbstractIntegrationTest {

    @Test
    @DisabledOnJre({ JRE.JAVA_8, JRE.JAVA_11 })
    void testRecordsSuppressions() {
        performAnalysis("../java17/Issue3471.class",
                "../java17/Issue3471$Issue3471ExposeRep.class",
                "../java17/Issue3471$Issue3471Suppressed.class");

        assertBugInClassCount("EI_EXPOSE_REP", "Issue3471$Issue3471Suppressed", 0);
        assertBugInClassCount("EI_EXPOSE_REP2", "Issue3471$Issue3471Suppressed", 0);
        assertBugInClassCount("US_USELESS_SUPPRESSION_ON_FIELD", "Issue3471$Issue3471Suppressed", 0);
        assertBugInClassCount("US_USELESS_SUPPRESSION_ON_METHOD", "Issue3471$Issue3471Suppressed", 0);
        assertBugInClassCount("US_USELESS_SUPPRESSION_ON_METHOD_PARAMETER", "Issue3471$Issue3471Suppressed", 0);

        assertBugInClassCount("EI_EXPOSE_REP", "Issue3471$Issue3471ExposeRep", 1);
        assertBugInClassCount("EI_EXPOSE_REP2", "Issue3471$Issue3471ExposeRep", 1);
    }
}
