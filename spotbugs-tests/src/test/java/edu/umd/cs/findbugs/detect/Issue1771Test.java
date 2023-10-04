package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

class Issue1771Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("../java11/ghIssues/Issue1771.class");
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("EI_EXPOSE_REP").build();
        assertThat(getBugCollection(), containsExactly(0, matcher));
    }
}
