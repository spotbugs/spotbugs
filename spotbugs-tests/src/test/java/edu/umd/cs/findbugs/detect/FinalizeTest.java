package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class FinalizeTest extends AbstractIntegrationTest {
    @Test
    void testFinalize() {
        performAnalysis("Finalize.class");

        assertBugTypeCount("FI_EMPTY", 1);
        assertBugInMethod("FI_EMPTY", "Finalize", "finalize");
    }

    @Test
    void testUselessFinalize() {
        performAnalysis("UselessFinalize.class");

        assertBugTypeCount("FI_USELESS", 1);
        assertBugInMethod("FI_USELESS", "UselessFinalize", "finalize");
    }
}
