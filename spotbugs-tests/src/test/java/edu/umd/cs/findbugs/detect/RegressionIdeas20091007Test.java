package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


class RegressionIdeas20091007Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2009_10_07.class");
        assertBugTypeCount("NP_GUARANTEED_DEREF", 2);
        assertBugTypeCount("NP_LOAD_OF_KNOWN_NULL_VALUE", 1);

        assertBugInMethod("NP_GUARANTEED_DEREF", "Ideas_2009_10_07", "f"); // line 16 or 17 non deterministic
        assertBugInMethod("NP_GUARANTEED_DEREF", "Ideas_2009_10_07", "f2"); // line 29 or 30 non deterministic
        assertBugInMethodAtLine("NP_LOAD_OF_KNOWN_NULL_VALUE", "Ideas_2009_10_07", "f", 10);
    }
}
