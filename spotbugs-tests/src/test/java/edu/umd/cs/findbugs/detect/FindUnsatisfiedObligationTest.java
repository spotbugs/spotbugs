package edu.umd.cs.findbugs.detect;

import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsExtension;
import edu.umd.cs.findbugs.test.SpotBugsRunner;

@ExtendWith(SpotBugsExtension.class)
class FindUnsatisfiedObligationTest {

    public SpotBugsExtension spotbugs = new SpotBugsExtension();

    /**
     * @see <a href="https://github.com/spotbugs/spotbugs/issues/60">GitHub
     *      issue</a>
     */
    @Test
    void testIssue60(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(Paths.get("../spotbugsTestCases/build/classes/java/main/lambdas/Issue60.class"));
        assertThat(bugCollection, is(emptyIterable()));
    }
}
