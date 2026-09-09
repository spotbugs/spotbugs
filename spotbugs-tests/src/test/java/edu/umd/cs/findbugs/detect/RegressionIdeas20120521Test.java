package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


class RegressionIdeas20120521Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2012_05_21.class");
        assertBugTypeCount("ES_COMPARING_STRINGS_WITH_EQ", 1);
        assertBugInMethodAtLine("ES_COMPARING_STRINGS_WITH_EQ", "Ideas_2012_05_21", "equals", 21);
    }
}
