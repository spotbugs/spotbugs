package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsBetween;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class RegressionIdeas20091005Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2009_10_05.class");
        assertBug("NP_NONNULL_PARAM_VIOLATION", 2, 14);
        // FNs in lines 10-15 and 20-26

        assertExactBug("NP_NONNULL_PARAM_VIOLATION", "propertiesCantContainNull", 9);
        assertExactBug("NP_NONNULL_PARAM_VIOLATION", "hashtablesCantContainNull", 20);
    }

    private void assertBug(String bugtype, int min, int max) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .build();
        assertThat(getBugCollection(), containsBetween(min, max, matcher));
    }

    private void assertExactBug(String bugtype, String method, int line) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .inClass("Ideas_2009_10_05")
                .inMethod(method)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }
}
