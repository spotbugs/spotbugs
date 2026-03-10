package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3460Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("infiniteLoop/Issue3460.class");

        // unconditional loops are assumed to be intentional
        assertBugTypeCount("IL_INFINITE_LOOP", 0);
    }
}
