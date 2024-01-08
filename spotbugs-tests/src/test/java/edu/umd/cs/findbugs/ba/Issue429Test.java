package edu.umd.cs.findbugs.ba;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsExtension;
import edu.umd.cs.findbugs.test.SpotBugsRunner;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

/**
 * Unit test to reproduce <a href="https://github.com/spotbugs/spotbugs/issues/429">#429</a>.
 */
@ExtendWith(SpotBugsExtension.class)
class Issue429Test {

    @Test
    void testIssue(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/ghIssues/Issue429.class"));
        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder().bugType("RV_RETURN_VALUE_IGNORED")
                .build();
        assertThat(bugCollection, containsExactly(0, matcher));
    }
}
