package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class SBConcatTest extends AbstractIntegrationTest {
    @Test
    void testSBConcatTest() {
        performAnalysis("SBConcatTest.class");

        assertBugTypeCount("SBSC_USE_STRINGBUFFER_CONCATENATION", 2);
        assertBugInMethod("SBSC_USE_STRINGBUFFER_CONCATENATION", "SBConcatTest", "doConcat1");
        assertBugInMethod("SBSC_USE_STRINGBUFFER_CONCATENATION", "SBConcatTest", "doConcat2");
    }
}
