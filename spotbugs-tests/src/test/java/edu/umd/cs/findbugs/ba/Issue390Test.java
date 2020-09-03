package edu.umd.cs.findbugs.ba;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

import org.hamcrest.MatcherAssert;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;

public class Issue390Test extends AbstractIntegrationTest {

    @Test
    public void test() {
        performAnalysis("ghIssues/Issue390.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().bugType("JUA_DONT_ASSERT_INSTANCEOF_IN_TESTS").build();
        MatcherAssert.assertThat(getBugCollection(), containsExactly(1, bugTypeMatcher));
    }

}
