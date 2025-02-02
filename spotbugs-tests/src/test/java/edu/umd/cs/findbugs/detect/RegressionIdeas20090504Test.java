package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


class RegressionIdeas20090504Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2009_05_04.class");
        assertBugTypeCount("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", 1);

        assertBugInMethodAtLine("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", "Ideas_2009_05_04", "foo", 11);
    }
}
