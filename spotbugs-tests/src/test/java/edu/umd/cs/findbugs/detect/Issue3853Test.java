package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue3853Test extends AbstractIntegrationTest {

    @Test
    @EnabledOnJre(JRE.JAVA_17)
    void testIssueJre17() {
        performAnalysis("../java17/Issue3853.class");

        assertBugInMethod("DM_DEFAULT_ENCODING", "Issue3853", "filesReader");
    }

    @Test
    @EnabledOnJre(JRE.JAVA_21)
    void testIssueJre21() {
        performAnalysis("../java21/Issue3853.class");

        assertNoBugInMethod("DM_DEFAULT_ENCODING", "Issue3853", "filesReader");
    }
}
