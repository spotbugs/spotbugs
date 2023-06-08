package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

public class RegressionIdeas20110722Test extends AbstractIntegrationTest {
    @Test
    public void testArgumentAssertions() {
        performAnalysis("bugIdeas/Ideas_2011_07_22.class");

        assertNumOfBugs("NP_NULL_ON_SOME_PATH", 0);
        assertNumOfBugs("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", 4);
        assertNumOfBugs("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", 2);

        assertNoNpBugInMethod("getHashCode");
        assertNoNpBugInMethod("getHashCode1");
        assertNoNpBugInMethod("getHashCode2");
        assertNoNpBugInMethod("getHashCode3");
        assertRCNBug("getHashCode3", "x", 34);
        assertNoNpBugInMethod("getHashCode4");
        assertRCNBug("getHashCode4", "x", 41);
        assertNoNpBugInMethod("getHashCode5");
        assertRCNBug("getHashCode5", "x", 48);
        assertRCNBug("getHashCode6", "x", 55);
        assertNpParamBug("getHashCode6", "x");
        assertNpParamBug("getHashCode7", "x");
    }

    private void assertNumOfBugs(String error, int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(error).build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertNoNpBugInMethod(String method) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("NP_NULL_ON_SOME_PATH")
                .inClass("Ideas_2011_07_22")
                .inMethod(method)
                .build();
        assertThat(getBugCollection(), containsExactly(0, bugInstanceMatcher));
    }

    private void assertRCNBug(String method, String var, int line) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
                .inClass("Ideas_2011_07_22")
                .inMethod(method)
                .atVariable(var)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }

    private void assertNpParamBug(String method, String var) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE")
                .inClass("Ideas_2011_07_22")
                .inMethod(method)
                .atVariable(var)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }
}
