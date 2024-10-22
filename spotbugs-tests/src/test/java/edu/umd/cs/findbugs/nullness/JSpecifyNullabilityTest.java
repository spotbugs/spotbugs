package edu.umd.cs.findbugs.nullness;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsExtension;
import edu.umd.cs.findbugs.test.SpotBugsRunner;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Paths;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;

/**
 * Check if annotation from https://jspecify.dev/ is detected.
 *
 */
@ExtendWith(SpotBugsExtension.class)
class JSpecifyNullabilityTest {

    @Test
    void checkedJSpecifyNullableReturn_isOk(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/CheckedJSpecifyNullableReturn.class"));
        assertThat(bugCollection, emptyIterable());
    }

    @Test
    void uncheckedJSpecifyNullableReturn_isDetected(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/UncheckedJSpecifyNullableReturn.class"));

        assertThat(bugCollection, containsExactly(1, bug()));
    }

    private BugInstanceMatcher bug() {
        return new BugInstanceMatcherBuilder().bugType("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE").build();
    }
}
