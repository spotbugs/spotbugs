package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3905Test extends AbstractIntegrationTest {

    @Test
    void testSfSwitchNoDefaultWithEmptyLastCase() {
        performAnalysis("ghIssues/Issue3905.class");

        // True positive: last case has logic - already detected before the fix
        assertBugInMethod("SF_SWITCH_NO_DEFAULT", "ghIssues.Issue3905", "lastCaseWithLogic");

        // False negative (now fixed): last case contains only a break
        assertBugInMethod("SF_SWITCH_NO_DEFAULT", "ghIssues.Issue3905", "lastCaseOnlyBreak");

        // Single case with only a break and no default
        assertBugInMethod("SF_SWITCH_NO_DEFAULT", "ghIssues.Issue3905", "singleCaseOnlyBreak");

        // Has explicit default - should NOT report
        assertNoBugInMethod("SF_SWITCH_NO_DEFAULT", "ghIssues.Issue3905", "lastCaseOnlyBreakWithDefault");

        // Has explicit empty default - should NOT report
        assertNoBugInMethod("SF_SWITCH_NO_DEFAULT", "ghIssues.Issue3905", "withExplicitEmptyDefault");
    }
}
