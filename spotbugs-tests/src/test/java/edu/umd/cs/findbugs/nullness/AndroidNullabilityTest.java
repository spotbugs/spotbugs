package edu.umd.cs.findbugs.nullness;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.Matchers.emptyIterable;
import static org.junit.Assert.assertThat;

import java.nio.file.Paths;

import org.junit.Rule;
import org.junit.Test;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsRule;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

/**
 * Check if NonNull and Nullable annotations from android.support.annotation are detected.
 *
 * @author kzaikin
 */
public class AndroidNullabilityTest {
    @Rule
    public SpotBugsRule spotbugs = new SpotBugsRule();

    @Test
    public void objectForNonNullParam_isOk() {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/androidAnnotations/ObjectForNonNullParam.class"));
        assertThat(bugCollection, emptyIterable());
    }

    @Test
    public void objectForNonNullParam2_isOk() {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/androidAnnotations/ObjectForNonNullParam2.class"));
        assertThat(bugCollection, emptyIterable());
    }

    @Test
    public void nullForNonNullParam_isDetected() {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/androidAnnotations/NullForNonNullParam.class"));

        assertThat(bugCollection, containsExactly(1, bug("NP_NONNULL_PARAM_VIOLATION")));
    }

    @Test
    public void nullForNonNullParam2_isDetected() {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/androidAnnotations/NullForNonNullParam2.class"));

        assertThat(bugCollection, containsExactly(1, bug("NP_NONNULL_PARAM_VIOLATION")));
    }

    @Test
    public void checkedNullableReturn_isOk() {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/androidAnnotations/CheckedNullableReturn.class"));
        assertThat(bugCollection, emptyIterable());
    }

    @Test
    public void checkedNullableReturn2_isOk() {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/androidAnnotations/CheckedNullableReturn2.class"));
        assertThat(bugCollection, emptyIterable());
    }

    @Test
    public void uncheckedNullableReturn_isDetected() {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/androidAnnotations/UncheckedNullableReturn.class"));

        assertThat(bugCollection, containsExactly(1, bug("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")));
    }

    @Test
    public void uncheckedNullableReturn2_isDetected() {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/androidAnnotations/UncheckedNullableReturn2.class"));

        assertThat(bugCollection, containsExactly(1, bug("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")));
    }

    private BugInstanceMatcher bug(String type) {
        return new BugInstanceMatcherBuilder().bugType(type).build();
    }
}
