package edu.umd.cs.findbugs.ba;

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
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/1367">The related GitHub issue</a>
 */
public class Issue1367Test {
    @Rule
    public SpotBugsRule spotbugs = new SpotBugsRule();

    @Test
    public void test() {
        BugCollection bugCollection = spotbugs.performAnalysis(Paths.get("../spotbugsTestCases/build/classes/java/java14/Issue1367.class"));
        BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder().bugType("EQ_UNUSUAL").build();
        assertThat(bugCollection, containsExactly(0, bugTypeMatcher));
    }
}
