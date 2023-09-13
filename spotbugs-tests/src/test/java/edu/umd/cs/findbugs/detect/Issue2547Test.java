package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;


/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/2547">GitHub issue #2547</a>
 */
public class Issue2547Test extends AbstractIntegrationTest {
    @Test
    public void test() {
        performAnalysis("ghIssues/issue2547/Issue2547.class",
                "ghIssues/issue2547/MyEx.class",
                "ghIssues/issue2547/ExceptionFactory.class");

        assertBug(3);
        assertBug("notThrowingExCtor", 15);
        assertBug("notThrowingExCtorCaller", 26);
        assertBug("notThrowingExCtorCallerOutside", 37);
    }

    private void assertBug(int num) {
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("RV_EXCEPTION_NOT_THROWN")
                .build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertBug(String method, int line) {
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("RV_EXCEPTION_NOT_THROWN")
                .inClass("Issue2547")
                .inMethod(method)
                .atLine(line)
                .build();
        assertThat(getBugCollection(), hasItem(bugTypeMatcher));
    }
}
