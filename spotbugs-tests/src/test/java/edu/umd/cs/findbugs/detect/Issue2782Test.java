package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

class Issue2782Test extends AbstractIntegrationTest {

    @Test
    @DisabledOnJre({ JRE.JAVA_8, JRE.JAVA_11, JRE.JAVA_17 })
    void testIssue() {
        performAnalysis(
                "../java21/Issue2782.class",
                "../java21/Issue2782$MyInterface.class",
                "../java21/Issue2782$Impl1.class",
                "../java21/Issue2782$Impl2.class");

        // Checks for issue 2782
        assertBugTypeCount("BC_UNCONFIRMED_CAST", 1);
        assertBugTypeCount("BC_UNCONFIRMED_CAST_OF_RETURN_VALUE", 1);

        assertBugAtLine("BC_UNCONFIRMED_CAST", 52);
        assertBugAtLine("BC_UNCONFIRMED_CAST_OF_RETURN_VALUE", 51);

        // Checks for issue 2736
        assertBugTypeCount("DLS_DEAD_LOCAL_STORE", 1);

        assertBugAtLine("DLS_DEAD_LOCAL_STORE", 61);
    }
}
