package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class RegressionIdeas20091006Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2009_10_06.class");
        assertBug("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS", 4);

        assertExactBug("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS", "testFirstArg", 21);
        assertExactBug("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS", "testFirstArg2", 29);
        assertExactBug("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS", "testSecondArg", 25);
        assertExactBug("NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS", "testSecondArg2", 33);
    }

    private void assertBug(String bugtype, int num) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .build();
        assertThat(getBugCollection(), containsExactly(num, matcher));
    }

    private void assertExactBug(String bugtype, String method, int line) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .inClass("Ideas_2009_10_06")
                .inMethod(method)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }
}
