package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


class RegressionIdeas20090526Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2009_05_26.class");
        assertBugTypeCount("NM_METHOD_CONSTRUCTOR_CONFUSION", 1);
        assertBugTypeCount("NM_METHOD_NAMING_CONVENTION", 1);

        assertBugInMethod("NM_METHOD_CONSTRUCTOR_CONFUSION", "Ideas_2009_05_26", "Ideas_2009_05_26");
        assertBugInMethod("NM_METHOD_NAMING_CONVENTION", "Ideas_2009_05_26", "Ideas_2009_05_26");

        // There should be also some unrelated hits in the main function, but there isn't any
    }
}
