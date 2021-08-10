package edu.umd.cs.findbugs.nullness;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;

import java.nio.file.Paths;

import org.junit.Rule;
import org.junit.Test;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsRule;

/**
 * Unit test to reproduce <a href="https://github.com/spotbugs/spotbugs/issues/259">#259</a>.
 */
public class Issue259Test {
    @Rule
    public SpotBugsRule spotbugs = new SpotBugsRule();

    @Test
    public void test() {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/ghIssues/Issue259.class"),
                Paths.get("../spotbugsTestCases/build/classes/java/main/ghIssues/Issue259$1.class"),
                Paths.get("../spotbugsTestCases/build/classes/java/main/ghIssues/Issue259$X.class"));
        assertThat(bugCollection, emptyIterable());
    }
}
