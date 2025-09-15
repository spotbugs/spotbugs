package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

class Issue1148Test extends AbstractIntegrationTest {

    @Test
    @EnabledForJreRange(max = JRE.JAVA_20)
    void testIssueUnderJava21() {
        // Under JDK 21 there is an additional class in the bytecode generated from the switch
        performAnalysis("ghIssues/Issue1148.class",
                "ghIssues/Issue1148$UsageType.class", "ghIssues/Issue1148$1.class");

        assertBugInMethodCount("SF_SWITCH_NO_DEFAULT", "ghIssues.Issue1148", "addData", 0);
    }

    @Test
    @EnabledForJreRange(min = JRE.JAVA_21)
    void testIssueJava21() {
        performAnalysis("ghIssues/Issue1148.class",
                "ghIssues/Issue1148$UsageType.class");

        assertBugInMethodCount("SF_SWITCH_NO_DEFAULT", "ghIssues.Issue1148", "addData", 0);
    }

    @Test
    @Disabled("This test is disabled because the false positive is still detected")
    void testUnresolvedFalsePositive() {
        performAnalysis("ghIssues/Issue1148.class",
                "ghIssues/Issue1148$UsageType.class");

        assertBugInMethodCount("SF_SWITCH_NO_DEFAULT", "ghIssues.Issue1148", "setDamageParam", 0);
    }
}
