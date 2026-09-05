package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3461Test extends AbstractIntegrationTest {

    @Test
    void testRandomValue() {
        performAnalysis("ghIssues/Issue3461.class");

        assertBugTypeCount("RV_01_TO_INT", 1);
    }
}
