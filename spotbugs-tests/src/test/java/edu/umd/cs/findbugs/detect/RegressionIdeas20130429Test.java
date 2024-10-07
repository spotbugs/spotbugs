package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


class RegressionIdeas20130429Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2013_04_29.class");
        assertBugTypeCountBetween("SA_LOCAL_SELF_COMPUTATION", 1, 2);
        assertBugInMethodAtLine("SA_LOCAL_SELF_COMPUTATION", "Ideas_2013_04_29", "testSelfOperation", 8);

        // FN in testSelfOperationField
    }
}
