package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegressionIdeas20091010Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2009_10_10.class");
        assertBugNum("NP_LOAD_OF_KNOWN_NULL_VALUE", 1, 4);
        assertBugNum("NP_NONNULL_PARAM_VIOLATION", 1, 4);

        assertExactBug("NP_LOAD_OF_KNOWN_NULL_VALUE", 15);
        assertExactBug("NP_NONNULL_PARAM_VIOLATION", 15);
        // FNs in line 17, 18, 19
    }

    private void assertBugNum(String bugtype, int min, int max) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .build();
        long exactMatches = getBugCollection().getCollection().stream().filter(matcher::matches).count();
        assertTrue(exactMatches >= min);
        assertTrue(exactMatches <= max);
    }

    private void assertExactBug(String bugtype, int line) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .inClass("Ideas_2009_10_10")
                .inMethod("test")
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }
}
