package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3481Test extends AbstractIntegrationTest {

    @Test
    void testAccessMethodForMethod() {
        performAnalysis("../java8/Issue3481.class", "../java8/Issue3481$Nested.class");

        assertBugTypeCount("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", 1);
        assertBugInMethod("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "Issue3481$Nested", "foo");
    }
}
