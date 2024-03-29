package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

class Issue1759Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue1759.class");
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("BC_IMPOSSIBLE_DOWNCAST_OF_TOARRAY").build();
        assertThat(getBugCollection(), containsExactly(1, matcher));
    }
}
