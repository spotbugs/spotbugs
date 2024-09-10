package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class RegressionIdeas20091007Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2009_10_07.class");
        assertBug("NP_GUARANTEED_DEREF", 2);
        assertBug("NP_LOAD_OF_KNOWN_NULL_VALUE", 1);

        assertExactBug("NP_GUARANTEED_DEREF", "f"); // line 16 or 17 non deterministic
        assertExactBug("NP_GUARANTEED_DEREF", "f"); // line 29 or 30 non deterministic
        assertExactBugWithLine("NP_LOAD_OF_KNOWN_NULL_VALUE", "f", 10);
    }

    private void assertBug(String bugtype, int num) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .build();
        assertThat(getBugCollection(), containsExactly(num, matcher));
    }

    private void assertExactBugWithLine(String bugtype, String method, int line) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .inClass("Ideas_2009_10_07")
                .inMethod(method)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }

    private void assertExactBug(String bugtype, String method) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .inClass("Ideas_2009_10_07")
                .inMethod(method)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }
}
