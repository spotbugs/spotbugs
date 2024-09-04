package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class RegressionIdeas20090105Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2009_01_05.class");
        assertBugNum(2);
        assertExactBug("test", 25);
        assertExactBug("test2", 29);
    }

    private void assertBugNum(int num) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
                .build();
        assertThat(getBugCollection(), containsExactly(num, matcher));
    }

    private void assertExactBug(String method, int line) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS")
                .inClass("Ideas_2009_01_05")
                .inMethod(method)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }
}
