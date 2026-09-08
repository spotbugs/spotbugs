package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue3622Test extends AbstractIntegrationTest {

    @Test
    void testRecordsSuppressions() {
        performAnalysis("../java17/Issue3622.class");

        assertBugTypeCount("EQ_ALWAYS_TRUE", 0);
    }
}
