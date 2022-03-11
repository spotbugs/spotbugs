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
 * Unit tests to reproduce <a href="https://github.com/spotbugs/spotbugs/issues/600">#600</a>.
 */
public class Issue600Test {
    @Rule
    public SpotBugsRule spotbugs = new SpotBugsRule();

    @Test
    public void testTryWithResourcesSimple() {
        assertBugCount("TryWithResourcesSimple", 0);
        assertBugCount("TryWithResourcesMultiple", 0);
        assertBugCount("TryWithResourcesNested", 0);
    }

    @Test
    public void testExplicitNulls() {
        assertBugCount("ExplicitNullCheckSimple", 1);
        assertBugCount("ExplicitNullCheckNested", 2);
        assertBugCount("ExplicitNullCheckMultiple", 2);
        assertBugCount("Mixed1", 1);
        assertBugCount("Mixed2", 1);
    }

    private void assertBugCount(String clazz, int count) {
        BugCollection bugCollection = analyse(clazz);

        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE").build();
        assertThat(bugCollection, containsExactly(count, bugTypeMatcher));

        final BugInstanceMatcher bugTypeMatcher2 = new BugInstanceMatcherBuilder()
                .bugType("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE").build();
        assertThat(bugCollection, containsExactly(0, bugTypeMatcher2));
    }

    private BugCollection analyse(String clazz) {
        return spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/ghIssues/issue600/" + clazz + ".class"),
                Paths.get("../spotbugsTestCases/build/classes/java/main/ghIssues/issue600/MyAutocloseable.class"),
                Paths.get("../spotbugsTestCases/build/classes/java/main/ghIssues/issue600/IMyAutocloseable.class"));
    }
}
