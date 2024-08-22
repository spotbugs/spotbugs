package edu.umd.cs.findbugs.nullness;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsExtension;
import edu.umd.cs.findbugs.test.SpotBugsRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Paths;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;

/**
 * Unit test to reproduce <a href="https://github.com/spotbugs/spotbugs/issues/1338">#1338</a>.
 */
@ExtendWith(SpotBugsExtension.class)
class Issue1338Test {

    @Test
    void testIssue(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/ghIssues/Issue1338.class"));
        assertThat(bugCollection, emptyIterable());
    }
}
