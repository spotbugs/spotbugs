package edu.umd.cs.findbugs.detect;

import static org.hamcrest.MatcherAssert.assertThat;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.ExtendWith;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsExtension;
import edu.umd.cs.findbugs.test.SpotBugsRunner;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

@ExtendWith(SpotBugsExtension.class)
class IncorrectSelfComparisonInstanceOfPatternMatchingTest {

    /**
     * @see <a href="https://github.com/spotbugs/spotbugs/issues/1136">GitHub
     *      issue</a>
     */
    @Test
    @DisabledOnJre({ JRE.JAVA_8, JRE.JAVA_11 })
    void testIssue1136(SpotBugsRunner spotbugs) {
        final BugInstanceMatcher selfComparisonMatcher = new BugInstanceMatcherBuilder()
                .bugType("SA_LOCAL_SELF_COMPARISON").build();

        BugCollection bugCollection = spotbugs.performAnalysis(Paths.get(
                "../spotbugsTestCases/build/classes/java/java17/IncorrectSelfComparisonInstanceOfPatternMatching.class"));
        assertThat(bugCollection, containsExactly(0, selfComparisonMatcher));
    }
}
