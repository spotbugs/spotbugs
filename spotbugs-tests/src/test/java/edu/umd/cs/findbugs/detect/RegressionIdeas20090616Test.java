package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


class RegressionIdeas20090616Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2009_06_16.class");
        assertBugTypeCount("SA_FIELD_DOUBLE_ASSIGNMENT", 1);
        assertBugTypeCount("UWF_UNWRITTEN_FIELD", 1);

        assertBugAtLine("SA_FIELD_DOUBLE_ASSIGNMENT", 9);
        assertBugAtLine("UWF_UNWRITTEN_FIELD", 17);
    }
}
