package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class RegressionIdeas20090526Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2009_05_26.class");
        assertBug("NM_METHOD_CONSTRUCTOR_CONFUSION");
        assertBug("NM_METHOD_NAMING_CONVENTION");

        assertExactBug("NM_METHOD_CONSTRUCTOR_CONFUSION", "Ideas_2009_05_26");
        assertExactBug("NM_METHOD_NAMING_CONVENTION", "Ideas_2009_05_26");

        // There should be also some unrelated hits in the main function, but there isn't any
    }

    private void assertBug(String bugtype) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .build();
        assertThat(getBugCollection(), containsExactly(1, matcher));
    }

    private void assertExactBug(String bugtype, String method) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .inClass("Ideas_2009_05_26")
                .inMethod(method)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }
}
