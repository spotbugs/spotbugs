package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.junit.Assert.assertThat;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Test;

/** @see <a href="https://github.com/spotbugs/spotbugs/issues/744">GitHub issue #744</a> */
public class Issue744Test extends AbstractIntegrationTest {
    @Test
    public void test() {
        performAnalysis("ghIssues/Issue744.class");
        BugInstanceMatcher bugTypeMatcher =
                new BugInstanceMatcherBuilder().bugType("DM_BOXED_PRIMITIVE_FOR_PARSING").build();
        assertThat(getBugCollection(), containsExactly(4, bugTypeMatcher));
    }
}
