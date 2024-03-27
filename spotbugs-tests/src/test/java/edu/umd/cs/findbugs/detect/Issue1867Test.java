package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;


class Issue1867Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue1867.class");
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR")
                .build();

        assertThat(getBugCollection(), containsExactly(0, matcher));
    }
}
