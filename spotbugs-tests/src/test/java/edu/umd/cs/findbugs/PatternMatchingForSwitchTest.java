package edu.umd.cs.findbugs;

import edu.umd.cs.findbugs.test.SpotBugsExtension;
import edu.umd.cs.findbugs.test.SpotBugsRunner;

import java.nio.file.Path;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnJre;
import org.junit.jupiter.api.condition.JRE;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/1561">The related GitHub pull request</a>
 */
@ExtendWith(SpotBugsExtension.class)
class PatternMatchingForSwitchTest {

    @Test
    @EnabledOnJre(JRE.JAVA_21)
    public void test(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Path.of("../spotbugsTestCases/build/classes/java/java21/PatternMatchingForSwitch.class"),
                Path.of("../spotbugsTestCases/build/classes/java/java21/Shape.class"),
                Path.of("../spotbugsTestCases/build/classes/java/java21/Triangle.class"));
        Assertions.assertTrue(bugCollection.getCollection().isEmpty());
    }
}
