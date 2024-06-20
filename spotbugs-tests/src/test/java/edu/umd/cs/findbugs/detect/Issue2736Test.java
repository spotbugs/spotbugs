package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.*;

class Issue2736Test extends AbstractIntegrationTest {

    @Test
    @DisabledOnJre({ JRE.JAVA_8, JRE.JAVA_11, JRE.JAVA_17, JRE.JAVA_21 })
    void testIssue() {
        performAnalysis("../java22/Issue2736.class");

        assertBugCount(0);
    }

    private void assertBugCount(int expectedCount) {
        BugInstanceMatcher bugMatcher = new BugInstanceMatcherBuilder()
                .build();

        assertThat(getBugCollection(), containsExactly(expectedCount, bugMatcher));
    }
}
