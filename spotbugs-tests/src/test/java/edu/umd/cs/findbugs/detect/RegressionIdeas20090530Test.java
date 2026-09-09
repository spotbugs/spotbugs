package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


class RegressionIdeas20090530Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2009_05_30.class");
        assertBugTypeCount("SA_FIELD_SELF_ASSIGNMENT", 3);

        assertBugInMethodAtLine("SA_FIELD_SELF_ASSIGNMENT", "Ideas_2009_05_30", "copyInto", 10);
        assertBugInMethodAtLine("SA_FIELD_SELF_ASSIGNMENT", "Ideas_2009_05_30", "copy", 14);
        assertBugInMethodAtLine("SA_FIELD_SELF_ASSIGNMENT", "Ideas_2009_05_30", "messWith", 18);
    }
}
