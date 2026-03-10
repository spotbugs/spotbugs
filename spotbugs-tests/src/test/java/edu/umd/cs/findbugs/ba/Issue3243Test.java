package edu.umd.cs.findbugs.ba;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3243Test extends AbstractIntegrationTest {

    @Test
    void testBugSample() {
        performAnalysis("ghIssues/Issue3243.class");

        assertNoBugType("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE");
    }
}
