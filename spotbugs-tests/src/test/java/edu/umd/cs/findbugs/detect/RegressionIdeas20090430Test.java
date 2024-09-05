package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class RegressionIdeas20090430Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2009_04_30.class");
        assertBug("EC_NULL_ARG");
        assertBug("BC_IMPOSSIBLE_CAST");
        assertBug("BC_IMPOSSIBLE_DOWNCAST");
        assertBug("BC_IMPOSSIBLE_DOWNCAST_OF_TOARRAY");

        assertExactBug("EC_NULL_ARG", 12);
        assertExactBug("BC_IMPOSSIBLE_DOWNCAST", 14);
        assertExactBug("BC_IMPOSSIBLE_DOWNCAST_OF_TOARRAY", 18);
        assertExactBug("BC_IMPOSSIBLE_CAST", 20);
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
                .inClass("Ideas_2009_04_30")
                .inMethod("main")
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }
}
