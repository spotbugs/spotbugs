package edu.umd.cs.findbugs.ba;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

class Issue371Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("lambdas/Issue371.class");
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE").build();
        assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher));
    }

}
