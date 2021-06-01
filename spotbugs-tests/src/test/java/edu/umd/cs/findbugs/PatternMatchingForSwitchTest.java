package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.test.SpotBugsRule;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Paths;

import static org.junit.Assert.assertTrue;


/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/1561">The related GitHub pull request</a>
 */
public class PatternMatchingForSwitchTest {
    @Rule
    public SpotBugsRule spotbugs = new SpotBugsRule();

    @Test
    public void test() {
        BugCollection bugCollection = spotbugs.performAnalysis(Paths.get(
                "../spotbugsTestCases/build/classes/java/java17/PatternMatchingForSwitch.class"));
        assertTrue(bugCollection.getCollection().isEmpty());
    }
}
