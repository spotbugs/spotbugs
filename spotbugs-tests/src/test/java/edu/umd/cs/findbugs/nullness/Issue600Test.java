package edu.umd.cs.findbugs.nullness;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsExtension;
import edu.umd.cs.findbugs.test.SpotBugsRunner;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Paths;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests to reproduce <a href="https://github.com/spotbugs/spotbugs/issues/600">#600</a>.
 */
@ExtendWith(SpotBugsExtension.class)
class Issue600Test {

    @Test
    void testTryWithResourcesSimple(SpotBugsRunner spotbugs) {
        assertBugCount("TryWithResourcesSimple", 0, spotbugs);
        assertBugCount("TryWithResourcesMultiple", 0, spotbugs);
        assertBugCount("TryWithResourcesNested", 0, spotbugs);
    }

    @Test
    void testExplicitNulls(SpotBugsRunner spotbugs) {
        assertBugCount("ExplicitNullCheckSimple", 1, spotbugs);
        assertBugCount("ExplicitNullCheckNested", 2, spotbugs);
        assertBugCount("ExplicitNullCheckMultiple", 2, spotbugs);
        assertBugCount("Mixed1", 1, spotbugs);
        assertBugCount("Mixed2", 1, spotbugs);
    }

    private void assertBugCount(String clazz, int count, SpotBugsRunner spotbugs) {
        BugCollection bugCollection = analyse(clazz, spotbugs);

        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE").build();
        assertThat(bugCollection, containsExactly(count, bugTypeMatcher));

        final BugInstanceMatcher bugTypeMatcher2 = new BugInstanceMatcherBuilder()
                .bugType("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE").build();
        assertThat(bugCollection, containsExactly(0, bugTypeMatcher2));
    }

    private BugCollection analyse(String clazz, SpotBugsRunner spotbugs) {
        return spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/ghIssues/issue600/" + clazz + ".class"),
                Paths.get("../spotbugsTestCases/build/classes/java/main/ghIssues/issue600/MyAutocloseable.class"),
                Paths.get("../spotbugsTestCases/build/classes/java/main/ghIssues/issue600/IMyAutocloseable.class"));
    }
}
