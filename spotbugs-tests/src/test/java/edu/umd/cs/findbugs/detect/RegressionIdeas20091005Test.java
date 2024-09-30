package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


class RegressionIdeas20091005Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2009_10_05.class");
        assertBugTypeCountBetween("NP_NONNULL_PARAM_VIOLATION", 2, 14);
        // FNs in lines 10-15 and 20-26

        assertBugInMethodAtLine("NP_NONNULL_PARAM_VIOLATION", "Ideas_2009_10_05", "propertiesCantContainNull", 9);
        assertBugInMethodAtLine("NP_NONNULL_PARAM_VIOLATION", "Ideas_2009_10_05", "hashtablesCantContainNull", 20);
    }
}
