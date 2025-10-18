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

/**
 * Check if Nonnull annotation from jakarta.annotations is detected.
 *
 * @author pkini07
 */
@ExtendWith(SpotBugsExtension.class)
class JakartaNonnullTest {
    @Test
    void checkedJakartaNonnullReturn_isOk(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/CheckedJakartaNonnullReturn.class"));
        assertThat(bugCollection, containsExactly(0, bug()));
    }

    @Test
    void uncheckedJakartaNonnullReturn_isDetected(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/UncheckedJakartaNonnullReturn.class"));
        assertThat(bugCollection, containsExactly(1, bug()));
    }

    private BugInstanceMatcher bug() {
        return new BugInstanceMatcherBuilder().bugType("NP_NONNULL_RETURN_VIOLATION").build();
    }
}
