package edu.umd.cs.findbugs.detect;

import static org.hamcrest.collection.IsEmptyIterable.emptyIterable;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.nio.file.Paths;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.annotations.DischargesObligation;
import edu.umd.cs.findbugs.test.SpotBugsRule;

public class FindUnsatisfiedObligationTest {
    @Rule
    public SpotBugsRule spotbugs = new SpotBugsRule();

    /**
     * @see <a href="https://github.com/spotbugs/spotbugs/issues/60">GitHub
     *      issue</a>
     */
    @Test
    @Ignore
    public void testIssue60() {
        BugCollection bugCollection = spotbugs.performAnalysis(Paths.get("../spotbugsTestCases/build/classes/main/Issue60.class"));
        assertThat(bugCollection, is(emptyIterable()));
    }
}
