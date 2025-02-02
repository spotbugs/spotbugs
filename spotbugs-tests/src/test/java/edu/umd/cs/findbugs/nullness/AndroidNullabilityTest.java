package edu.umd.cs.findbugs.nullness;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.MatcherAssert.assertThat;

import java.nio.file.Paths;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsExtension;
import edu.umd.cs.findbugs.test.SpotBugsRunner;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

/**
 * Check if NonNull and Nullable annotations from android.support.annotation are detected.
 *
 * @author kzaikin
 */
@ExtendWith(SpotBugsExtension.class)
class AndroidNullabilityTest {

    @Test
    void objectForNonNullParam_isOk(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/androidAnnotations/ObjectForNonNullParam.class"));
        assertThat(bugCollection, emptyIterable());
    }

    @Test
    void objectForNonNullParam2_isOk(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/androidAnnotations/ObjectForNonNullParam2.class"));
        assertThat(bugCollection, emptyIterable());
    }

    @Test
    void nullForNonNullParam_isDetected(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/androidAnnotations/NullForNonNullParam.class"));

        assertThat(bugCollection, containsExactly(1, bug("NP_NONNULL_PARAM_VIOLATION")));
    }

    @Test
    void nullForNonNullParam2_isDetected(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/androidAnnotations/NullForNonNullParam2.class"));

        assertThat(bugCollection, containsExactly(1, bug("NP_NONNULL_PARAM_VIOLATION")));
    }

    @Test
    void checkedNullableReturn_isOk(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/androidAnnotations/CheckedNullableReturn.class"));
        assertThat(bugCollection, emptyIterable());
    }

    @Test
    void checkedNullableReturn2_isOk(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/androidAnnotations/CheckedNullableReturn2.class"));
        assertThat(bugCollection, emptyIterable());
    }

    @Test
    void uncheckedNullableReturn_isDetected(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/androidAnnotations/UncheckedNullableReturn.class"));

        assertThat(bugCollection, containsExactly(1, bug("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")));
    }

    @Test
    void uncheckedNullableReturn2_isDetected(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/androidAnnotations/UncheckedNullableReturn2.class"));

        assertThat(bugCollection, containsExactly(1, bug("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")));
    }

    private BugInstanceMatcher bug(String type) {
        return new BugInstanceMatcherBuilder().bugType(type).build();
    }
}
