package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


class RegressionIdeas20090603Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2009_06_03.class");
        assertBugTypeCount("JLM_JSR166_UTILCONCURRENT_MONITORENTER", 1);

        assertBugInMethodAtLine("JLM_JSR166_UTILCONCURRENT_MONITORENTER", "Ideas_2009_06_03", "atomicPut", 20);

        // there should some hits in the main functions as well, but there isn't any
    }
}
