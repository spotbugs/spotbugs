package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class RegressionIdeas20090504Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2009_05_04.class");
        assertBug("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE");

        assertExactBug("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", 11);
    }

    private void assertBug(String bugtype) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .build();
        assertThat(getBugCollection(), containsExactly(1, matcher));
    }

    private void assertExactBug(String bugtype, int line) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .inClass("Ideas_2009_05_04")
                .inMethod("foo")
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }
}
