package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class RegressionIdeas20090314Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2009_03_14.class");
        assertBug("NP_NULL_ON_SOME_PATH_EXCEPTION", 1);
        assertBug("SF_SWITCH_NO_DEFAULT", 1);

        assertExactBug("NP_NULL_ON_SOME_PATH_EXCEPTION", 18);
        assertExactBug("SF_SWITCH_NO_DEFAULT", 7);
    }

    private void assertBug(String bugtype, int num) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .build();
        assertThat(getBugCollection(), containsExactly(num, matcher));
    }

    private void assertExactBug(String bugtype, int line) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .inClass("Ideas_2009_03_14")
                .inMethod("foo")
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }
}
