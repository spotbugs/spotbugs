package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/pull/1540">PR suggested this test case</a>
 */
@RunWith(Parameterized.class)
public class Issue1539Test extends AbstractIntegrationTest {
    private final String target;

    public Issue1539Test(String className) {
        this.target = String.format("ghIssues/issue1539/%s.class", className);
    }

    @Parameterized.Parameters
    public static String[] data() {
        return new String[] {
            "Issue1539Instance",
            "Issue1539Static",
            "Issue1539ThreadLocal",
            "Issue1539Argument"
        };
    }

    @Test
    public void testInstance() {
        performAnalysis(target);
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("DMI_RANDOM_USED_ONLY_ONCE")
                .build();
        assertThat(getBugCollection(), containsExactly(0, bugTypeMatcher));
    }
}
