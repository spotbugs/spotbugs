package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.*;

import edu.umd.cs.findbugs.test.SpotBugsRunner;
import edu.umd.cs.findbugs.test.SpotBugsExtension;

import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Paths;

@ExtendWith(SpotBugsExtension.class)
class Issue2782Test extends AbstractIntegrationTest {

    @Test
    void testIssue(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/src/classSamples/java21typeSwitchSample/Issue2782.class"),
                Paths.get("../spotbugsTestCases/src/classSamples/java21typeSwitchSample/Issue2782$MyInterface.class"),
                Paths.get("../spotbugsTestCases/src/classSamples/java21typeSwitchSample/Issue2782$Impl1.class"),
                Paths.get("../spotbugsTestCases/src/classSamples/java21typeSwitchSample/Issue2782$Impl2.class"));

        BugInstanceMatcher matcher = new BugInstanceMatcherBuilder()
                .bugType("BC_UNCONFIRMED_CAST")
                .build();

        assertThat(bugCollection, containsExactly(0, matcher));
    }
}
