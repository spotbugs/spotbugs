package edu.umd.cs.findbugs.nullness;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsRule;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Paths;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/1839">GitHub Issue</a>
 */
public class Issue1839Test {
    @Rule
    public SpotBugsRule spotbugs = new SpotBugsRule();

    @Test
    public void test() {
        BugCollection bugCollection = spotbugs.performAnalysis(Paths.get("../spotbugsTestCases/build/classes/java/java14/Issue1839.class"));
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().bugType("NP_NONNULL_RETURN_VIOLATION").build();
        assertThat(bugCollection, containsExactly(0, bugTypeMatcher));
    }
}
