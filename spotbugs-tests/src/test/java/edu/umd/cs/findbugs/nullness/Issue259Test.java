package edu.umd.cs.findbugs.nullness;

import static org.hamcrest.Matchers.emptyIterable;
import static org.junit.Assert.assertThat;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsRule;
import java.nio.file.Paths;
import org.junit.Rule;
import org.junit.Test;

/** Unit test to reproduce <a href="https://github.com/spotbugs/spotbugs/issues/259">#259</a>. */
public class Issue259Test {
    @Rule
    public SpotBugsRule spotbugs = new SpotBugsRule();

    @Test
    public void test() {
        BugCollection bugCollection =
                spotbugs.performAnalysis(
                        Paths.get("../spotbugsTestCases/build/classes/java/main/ghIssues/Issue259.class"));
        assertThat(bugCollection, emptyIterable());
    }
}
