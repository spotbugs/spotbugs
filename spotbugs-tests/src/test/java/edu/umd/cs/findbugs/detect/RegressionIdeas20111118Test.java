package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


class RegressionIdeas20111118Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2011_11_18.class", "bugIdeas/Ideas_2011_11_18$1.class");
        assertBugTypeCount("UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS", 1);
        assertBugInMethod("UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS", "Ideas_2011_11_18$1", "get");
    }
}
