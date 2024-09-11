package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;

class RegressionIdeas20111118Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("bugIdeas/Ideas_2011_11_18.class", "bugIdeas/Ideas_2011_11_18$1.class");
        assertBug("UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS", 1);
        assertExactBug("UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS", "get");
    }

    private void assertBug(String bugtype, int num) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .build();
        assertThat(getBugCollection(), containsExactly(num, matcher));
    }

    private void assertExactBug(String bugtype, String method) {
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType(bugtype)
                .inClass("Ideas_2011_11_18$1")
                .inMethod(method)
                .build();
        assertThat(getBugCollection(), hasItem(matcher));
    }
}
