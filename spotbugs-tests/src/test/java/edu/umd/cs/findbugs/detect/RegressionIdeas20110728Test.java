package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


class RegressionIdeas20110728Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2011_07_28.class");
        assertBugTypeCount("EC_UNRELATED_TYPES", 1);
        assertBugInMethodAtLine("EC_UNRELATED_TYPES", "Ideas_2011_07_28", "testWillAlwaysFail", 8);
    }
}
