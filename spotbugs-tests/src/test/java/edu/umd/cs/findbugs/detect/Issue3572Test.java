package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

class Issue3572Test extends AbstractIntegrationTest {

    @Test
    @EnabledForJreRange(max = JRE.JAVA_20)
    void testIssueUnderJava21() {
        // Under JDK 21 there is an additional class in the bytecode generated from the switch
        performAnalysis("ghIssues/Issue3572.class",
                "ghIssues/Issue3572$MyEnum.class", "ghIssues/Issue3572$1.class");

        assertBugInMethodCount("SF_SWITCH_NO_DEFAULT", "ghIssues.Issue3572", "withSwitchDefault", 0);
        assertBugInMethodCount("SF_SWITCH_NO_DEFAULT", "ghIssues.Issue3572", "missingSwitchDefault", 1);
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_21)
    void testIssueJava21() {
        performAnalysis("ghIssues/Issue3572.class",
                "ghIssues/Issue3572$MyEnum.class");

        assertBugInMethodCount("SF_SWITCH_NO_DEFAULT", "ghIssues.Issue3572", "withSwitchDefault", 0);
        assertBugInMethodCount("SF_SWITCH_NO_DEFAULT", "ghIssues.Issue3572", "missingSwitchDefault", 1);
    }
}
