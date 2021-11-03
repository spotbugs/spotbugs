package edu.umd.cs.findbugs.detect;

import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.file.Paths;

import org.junit.Rule;
import org.junit.Test;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsRule;

public class FindUnsatisfiedObligationTest {
    @Rule
    public SpotBugsRule spotbugs = new SpotBugsRule();

    /**
     * @see <a href="https://github.com/spotbugs/spotbugs/issues/60">GitHub
     *      issue</a>
     */
    @Test
    public void testIssue60() {
        BugCollection bugCollection = spotbugs.performAnalysis(Paths.get("../spotbugsTestCases/build/classes/java/main/lambdas/Issue60.class"));
        assertThat(bugCollection, is(emptyIterable()));
    }
}
