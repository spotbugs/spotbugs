package edu.umd.cs.findbugs.nullness;

import edu.umd.cs.findbugs.BugCollection;
import edu.umd.cs.findbugs.test.SpotBugsExtension;
import edu.umd.cs.findbugs.test.SpotBugsRunner;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;

import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Paths;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.MatcherAssert.assertThat;


/**
 * Check if annotation from https://jspecify.dev/ is detected.
 *
 */
@ExtendWith(SpotBugsExtension.class)
class JSpecifyNullabilityTest {

    @Test
    void checkedJSpecifyNullableReturn_isOk(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/nullnessAnnotations/jspecify/CheckedJSpecifyNullableReturn.class"));
        assertThat(bugCollection, containsExactly(0, bug()));
    }

    @Test
    void checkedJSpecifyNullUnmarkedReturn_isOk(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/nullnessAnnotations/jspecify/CheckedJSpecifyNullUnmarkedReturn.class"));
        assertThat(bugCollection, containsExactly(0, bug()));
    }

    @Test
    void uncheckedJSpecifyNullableReturn_isDetected(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/nullnessAnnotations/jspecify/UncheckedJSpecifyNullableReturn.class"));

        assertThat(bugCollection, containsExactly(1, bug()));
    }


    @Test
    void checkedJSpecifyNonNull(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/nullnessAnnotations/jspecify/TestJSpecifyNonNull.class"));
        assertThat(bugCollection, containsExactly(1, paramBug()));
    }

    @Test
    @Disabled("Find bugs does not support enclosed non-null yet.  Need to implement enclosed as per https://jspecify.dev/docs/spec/#null-marked-scope ")
    void checkedJSpecifyNonNullEnclosed(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/nullnessAnnotations/jspecify/TestJSpecifyNonNullEnclosed.class"));
        assertThat(bugCollection, containsExactly(1, paramBug()));
    }


    @Test
    @Disabled("Find bugs does not support package-info.java NullMarked default.  Need to implement as per https://jspecify.dev/docs/user-guide/#nullmarked ")
    void checkedJSpecifyNullMarked(SpotBugsRunner spotbugs) {
        BugCollection bugCollection = spotbugs.performAnalysis(
                Paths.get("../spotbugsTestCases/build/classes/java/main/nullnessAnnotations/jspecify/nullmarked/TestJSpecifyNullMarked.class"));
        assertThat(bugCollection, containsExactly(1, paramBug()));
    }



    private BugInstanceMatcher bug() {
        return new BugInstanceMatcherBuilder().bugType("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE").build();
    }

    private BugInstanceMatcher paramBug() {
        return new BugInstanceMatcherBuilder().bugType("NP_NONNULL_PARAM_VIOLATION").build();
    }

}
