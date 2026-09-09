package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


class RegressionIdeas20090317Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2009_03_17.class");
        assertBugTypeCount("RC_REF_COMPARISON", 1);
        assertBugTypeCount("RC_REF_COMPARISON_BAD_PRACTICE", 1);

        assertBugInMethodAtLine("RC_REF_COMPARISON", "Ideas_2009_03_17", "areEqual", 12);
        assertBugInMethodAtLine("RC_REF_COMPARISON_BAD_PRACTICE", "Ideas_2009_03_17", "isSpecial", 8);
    }
}
