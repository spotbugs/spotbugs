package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RegressionIdeas20130429Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2013_04_29.class");
        assertBugNum("SA_LOCAL_SELF_COMPUTATION", 1, 2);
        assertExactBug("SA_LOCAL_SELF_COMPUTATION", "testSelfOperation", 8);

        // FN in testSelfOperationField
    }

    private void assertBugNum(String bugtype, int min, int max) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .build();
        long exactMatches = getBugCollection().getCollection().stream().filter(matcher::matches).count();
        assertTrue(exactMatches >= min);
        assertTrue(exactMatches <= max);
    }

    private void assertExactBug(String bugtype, String method, int line) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .inClass("Ideas_2013_04_29")
                .inMethod(method)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }
}
