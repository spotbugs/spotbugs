package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


class RegressionIdeas20090314Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2009_03_14.class");
        assertBugTypeCount("NP_NULL_ON_SOME_PATH_EXCEPTION", 1);
        assertBugTypeCount("SF_SWITCH_NO_DEFAULT", 1);

        assertBugInMethodAtLine("NP_NULL_ON_SOME_PATH_EXCEPTION", "Ideas_2009_03_14", "foo", 18);
        assertBugInMethodAtLine("SF_SWITCH_NO_DEFAULT", "Ideas_2009_03_14", "foo", 7);
    }
}
