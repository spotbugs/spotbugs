package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


class RegressionIdeas20090726Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2009_07_26.class");
        assertBugTypeCount("EQ_GETCLASS_AND_CLASS_CONSTANT", 1);
        assertBugTypeCount("NP_EQUALS_SHOULD_HANDLE_NULL_ARGUMENT", 1);
        assertBugTypeCount("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", 1);

        assertBugInMethod("EQ_GETCLASS_AND_CLASS_CONSTANT", "Ideas_2009_07_26", "equals");
        assertBugInMethod("NP_EQUALS_SHOULD_HANDLE_NULL_ARGUMENT", "Ideas_2009_07_26", "equals");
        assertBugInMethod("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", "Ideas_2009_07_26", "getHash");
    }
}
