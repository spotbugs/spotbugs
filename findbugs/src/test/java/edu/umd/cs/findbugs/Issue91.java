package edu.umd.cs.findbugs;

import static org.junit.Assert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/91">Issue 91</a>
 */
public class Issue91 extends AbstractIntegrationTest {
    @Test
    public void test() {
        performAnalysis("UselessCode.class");
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("UC_USELESS_CONDITION").build();
        assertThat(getBugCollection(), containsExactly(bugTypeMatcher, 0));
    }
}
