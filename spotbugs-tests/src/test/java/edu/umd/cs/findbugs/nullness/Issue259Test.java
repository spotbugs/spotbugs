package edu.umd.cs.findbugs.nullness;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsExtension;
import edu.umd.cs.findbugs.test.SpotBugsRunner;

/**
 * Unit test to reproduce <a href="https://github.com/spotbugs/spotbugs/issues/259">#259</a>.
 */
@ExtendWith(SpotBugsExtension.class)
class Issue259Test {

    @Test
    void testIssue(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/ghIssues/Issue259.class"),
                Paths.get("../spotbugsTestCases/build/classes/java/main/ghIssues/Issue259$1.class"),
                Paths.get("../spotbugsTestCases/build/classes/java/main/ghIssues/Issue259$X.class"));
        assertThat(bugCollection, emptyIterable());
    }
}
