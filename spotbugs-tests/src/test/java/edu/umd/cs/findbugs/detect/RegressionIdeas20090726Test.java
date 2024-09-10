package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class RegressionIdeas20090726Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2009_07_26.class");
        assertBug("EQ_GETCLASS_AND_CLASS_CONSTANT", 1);
        assertBug("NP_EQUALS_SHOULD_HANDLE_NULL_ARGUMENT", 1);
        assertBug("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", 1);

        assertExactBug("EQ_GETCLASS_AND_CLASS_CONSTANT", "equals");
        assertExactBug("NP_EQUALS_SHOULD_HANDLE_NULL_ARGUMENT", "equals");
        assertExactBug("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", "getHash");
    }

    private void assertBug(String bugtype, int expectedCount) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .build();
        assertThat(getBugCollection(), containsExactly(expectedCount, matcher));
    }

    private void assertExactBug(String bugtype, String method) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .inClass("Ideas_2009_07_26")
                .inMethod(method)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }
}
