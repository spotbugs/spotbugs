package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


class RegressionIdeas20091006Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2009_10_06.class");
        assertBugTypeCount("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS", 4);

        assertBugInMethodAtLine("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS", "Ideas_2009_10_06", "testFirstArg", 21);
        assertBugInMethodAtLine("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS", "Ideas_2009_10_06", "testFirstArg2", 29);
        assertBugInMethodAtLine("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS", "Ideas_2009_10_06", "testSecondArg", 25);
        assertBugInMethodAtLine("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS", "Ideas_2009_10_06", "testSecondArg2", 33);
    }
}
