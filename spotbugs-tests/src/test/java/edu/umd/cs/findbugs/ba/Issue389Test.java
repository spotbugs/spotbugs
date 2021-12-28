package edu.umd.cs.findbugs.ba;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class Issue389Test extends AbstractIntegrationTest {

    @Test
    public void test() {
        performAnalysis("ghIssues/Issue389.class");
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().bugType("UC_USELESS_VOID_METHOD").build();
        assertThat(getBugCollection(), containsExactly(3, bugTypeMatcher));
        bugTypeMatcher = new BugInstanceMatcherBuilder().bugType("DLS_DEAD_LOCAL_STORE").build();
        assertThat(getBugCollection(), containsExactly(3, bugTypeMatcher));
    }

}
