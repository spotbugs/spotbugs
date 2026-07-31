package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue3622Test extends AbstractIntegrationTest {

    @Test
    @EnabledForJreRange(min = JRE.JAVA_17)
    void testRecordsSuppressions() {
        performAnalysis("../java17/Issue3622.class");

        assertBugTypeCount("EQ_ALWAYS_TRUE", 0);
    }
}
