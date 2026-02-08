package edu.umd.cs.findbugs.nullness;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Disabled;
import edu.umd.cs.findbugs.AbstractIntegrationTest;

/**
 * Check if annotation from https://jspecify.dev/ is detected.
 *
 */
class JSpecifyNullabilityTest extends AbstractIntegrationTest {

    @Test
    void checkedJSpecifyNullableReturn_isOk() {
        performAnalysis(
                "nullnessAnnotations/jspecify/CheckedJSpecifyNullableReturn.class");
        assertNoBugType("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE");
    }

    @Test
    void checkedJSpecifyNullUnmarkedReturn_isOk() {
        performAnalysis(
                "nullnessAnnotations/jspecify/CheckedJSpecifyNullUnmarkedReturn.class");
        assertNoBugType("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE");
    }

    @Test
    void uncheckedJSpecifyNullableReturn_isDetected() {
        performAnalysis(
                "nullnessAnnotations/jspecify/UncheckedJSpecifyNullableReturn.class");
        assertBugTypeCount("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", 1);
    }


    @Test
    void checkedJSpecifyNonNull() {
        performAnalysis(
                "nullnessAnnotations/jspecify/TestJSpecifyNonNull.class");
        assertBugTypeCount("NP_NONNULL_PARAM_VIOLATION", 1);
        assertBugInMethodCount("NP_NONNULL_PARAM_VIOLATION", "TestJSpecifyNonNull", "bar", 1);
    }

    @Test
    @Disabled("Find bugs does not support enclosed non-null yet.  Need to implement enclosed as per https://jspecify.dev/docs/spec/#null-marked-scope ")
    void checkedJSpecifyNonNullEnclosed() {
        performAnalysis(
                "nullnessAnnotations/jspecify/TestJSpecifyNonNullEnclosed.class");
        assertBugTypeCount("NP_NONNULL_PARAM_VIOLATION", 1);
        assertBugInMethodCount("NP_NONNULL_PARAM_VIOLATION", "TestJSpecifyNonNullEnclosed", "bar", 1);
    }


    @Test
    @Disabled("Find bugs does not support package-info.java NullMarked default.  Need to implement as per https://jspecify.dev/docs/user-guide/#nullmarked ")
    void checkedJSpecifyNullMarked() {
        performAnalysis(
                "nullnessAnnotations/jspecify/nullmarked/TestJSpecifyNullMarked.class");
        assertBugTypeCount("NP_NONNULL_PARAM_VIOLATION", 1);
        assertBugInMethodCount("NP_NONNULL_PARAM_VIOLATION", "TestJSpecifyNullMarked", "bar", 1);
    }
}
