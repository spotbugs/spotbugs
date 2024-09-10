package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class RegressionIdeas20090616Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2009_06_16.class");
        assertBug("SA_FIELD_DOUBLE_ASSIGNMENT", 1);
        assertBug("UWF_UNWRITTEN_FIELD", 1);

        assertExactBug("SA_FIELD_DOUBLE_ASSIGNMENT", "Ideas_2009_06_16", 9);
        assertExactBug("UWF_UNWRITTEN_FIELD", "getY", 17);
    }

    private void assertBug(String bugtype, int expectedCount) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .build();
        assertThat(getBugCollection(), containsExactly(expectedCount, matcher));
    }

    private void assertExactBug(String bugtype, String method, int line) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .inClass("Ideas_2009_06_16")
                .inMethod(method)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }
}
