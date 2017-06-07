package edu.umd.cs.findbugs.nullness;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsRule;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.Rule;
import org.junit.Test;

import java.nio.file.Paths;

import static edu.umd.cs.findbugs.test.SpotBugsRule.containsExactly;
import static org.hamcrest.Matchers.emptyIterable;
import static org.junit.Assert.assertThat;

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
                Paths.get("../spotbugsTestCases/build/classes/main/androidAnnotations/ObjectForNonNullParam.class"));
        assertThat(bugCollection, emptyIterable());
    }

    @Test
    public void nullForNonNullParam_isDetected() {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/main/androidAnnotations/NullForNonNullParam.class"));

        assertThat(bugCollection, containsExactly(bug("NP_NONNULL_PARAM_VIOLATION"), 1));
    }

    @Test
    public void checkedNullableReturn_isOk() {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/main/androidAnnotations/CheckedNullableReturn.class"));
        assertThat(bugCollection, emptyIterable());
    }

    @Test
    public void uncheckedNullableReturn_isDetected() {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/main/androidAnnotations/UncheckedNullableReturn.class"));

        assertThat(bugCollection, containsExactly(bug("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE"), 1));
    }

    private BugInstanceMatcher bug(String type) {
        return new BugInstanceMatcherBuilder().bugType(type).build();
    }
}
