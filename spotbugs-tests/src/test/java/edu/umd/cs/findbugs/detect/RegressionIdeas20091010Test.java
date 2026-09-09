package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


class RegressionIdeas20091010Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2009_10_10.class");
        assertBugTypeCountBetween("NP_LOAD_OF_KNOWN_NULL_VALUE", 1, 4);
        assertBugTypeCountBetween("NP_NONNULL_PARAM_VIOLATION", 1, 4);

        assertBugInMethodAtLine("NP_LOAD_OF_KNOWN_NULL_VALUE", "Ideas_2009_10_10", "test", 15);
        assertBugInMethodAtLine("NP_NONNULL_PARAM_VIOLATION", "Ideas_2009_10_10", "test", 15);
        // FNs in line 17, 18, 19
    }
}
