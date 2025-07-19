package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3463Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue3463.class");

        assertBugInMethod("ASE_ASSERTION_WITH_SIDE_EFFECT", "ghIssues/Issue3463", "testField");
        assertBugInMethod("ASE_ASSERTION_WITH_SIDE_EFFECT", "ghIssues/Issue3463", "testStaticField");
    }
}
