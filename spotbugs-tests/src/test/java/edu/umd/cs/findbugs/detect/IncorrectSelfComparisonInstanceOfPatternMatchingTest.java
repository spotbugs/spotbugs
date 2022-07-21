package edu.umd.cs.findbugs.detect;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.number.OrderingComparison.greaterThanOrEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeThat;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;

import java.nio.file.Paths;

import org.junit.Rule;
import org.junit.Test;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsRule;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class IncorrectSelfComparisonInstanceOfPatternMatchingTest {
    @Rule
    public SpotBugsRule spotbugs = new SpotBugsRule();

    /**
     * @see <a href="https://github.com/spotbugs/spotbugs/issues/1136">GitHub
     *      issue</a>
     */
    @Test
    public void testIssue1136() {
        assumeFalse(System.getProperty("java.specification.version").startsWith("1."));
        int javaVersion = Integer.parseInt(System.getProperty("java.specification.version"));
        assumeThat(javaVersion, is(greaterThanOrEqualTo(14)));

        final BugInstanceMatcher selfComparisonMatcher = new BugInstanceMatcherBuilder()
                .bugType("SA_LOCAL_SELF_COMPARISON").build();

        BugCollection bugCollection = spotbugs.performAnalysis(Paths.get(
                "../spotbugsTestCases/build/classes/java/java14/IncorrectSelfComparisonInstanceOfPatternMatching.class"));
        assertThat(bugCollection, containsExactly(0, selfComparisonMatcher));
    }
}
