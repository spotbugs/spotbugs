package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue1405Test extends AbstractIntegrationTest {

    @Test
    void testUninitializedFieldDereferenceThroughGetter() {
        performAnalysis("ghIssues/Issue1405.class");
        assertBugInMethodAtLine("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "ghIssues.Issue1405", "main", 19);
    }
}
