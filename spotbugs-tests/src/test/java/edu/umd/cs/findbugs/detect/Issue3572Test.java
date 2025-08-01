package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;


class Issue3572Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue3572.class",
                "ghIssues/Issue3572$MyEnum.class");

        assertBugInMethodCount("SF_SWITCH_NO_DEFAULT", "ghIssues.Issue3572", "withSwitchDefault", 0);
        assertBugInMethodCount("SF_SWITCH_NO_DEFAULT", "ghIssues.Issue3572", "missingSwitchDefault", 1);
    }
}
