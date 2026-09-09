package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


class RegressionIdeas20090105Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2009_01_05.class");
        assertBugTypeCount("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS", 2);
        assertBugInMethodAtLine("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS", "Ideas_2009_01_05", "test", 25);
        assertBugInMethodAtLine("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS", "Ideas_2009_01_05", "test2", 29);
    }
}
