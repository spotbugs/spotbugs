package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3482Test extends AbstractIntegrationTest {

    @Test
    void testAccessMethodForMethod() {
        performAnalysis("../java8/Issue3482.class", "../java8/Issue3482$1.class");

        assertBugInMethod("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "Issue3482$1", "run");
    }
}
