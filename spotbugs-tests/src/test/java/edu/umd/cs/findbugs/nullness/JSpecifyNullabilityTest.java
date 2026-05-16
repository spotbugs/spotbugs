package edu.umd.cs.findbugs.nullness;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Check if annotations from https://jspecify.dev/ are detected.
 */
class JSpecifyNullabilityTest extends AbstractIntegrationTest {

    @Test
    void checkedJSpecifyNullableReturn_isOk() {
        performAnalysis(
                "nullnessAnnotations/jspecify/CheckedJSpecifyNullableReturn.class",
                "org/jspecify/annotations/Nullable.class");
        assertNoBugType("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE");
    }

    @Test
    void checkedJSpecifyNullUnmarkedReturn_isOk() {
        performAnalysis(
                "nullnessAnnotations/jspecify/CheckedJSpecifyNullUnmarkedReturn.class",
                "org/jspecify/annotations/NullUnmarked.class");
        assertNoBugType("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE");
    }

    @Test
    void uncheckedJSpecifyNullableReturn_isDetected() {
        performAnalysis(
                "nullnessAnnotations/jspecify/UncheckedJSpecifyNullableReturn.class",
                "org/jspecify/annotations/Nullable.class");
        assertBugTypeCount("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", 1);
        assertBugInMethod("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "UncheckedJSpecifyNullableReturn", "bar");
    }

    @Test
    void checkedJSpecifyNonNull() {
        performAnalysis(
                "nullnessAnnotations/jspecify/TestJSpecifyNonNull.class",
                "org/jspecify/annotations/NonNull.class");
        assertBugTypeCount("NP_NONNULL_PARAM_VIOLATION", 1);
        assertBugInMethodCount("NP_NONNULL_PARAM_VIOLATION", "TestJSpecifyNonNull", "bar", 1);
    }

    @Test
    @Disabled("SpotBugs does not yet support class-level @NullMarked as an enclosing scope default. "
            + "Full support requires implementing enclosed scope semantics per https://jspecify.dev/docs/spec/#null-marked-scope")
    void checkedJSpecifyNonNullEnclosed() {
        performAnalysis(
                "nullnessAnnotations/jspecify/TestJSpecifyNonNullEnclosed.class",
                "org/jspecify/annotations/NullMarked.class");
        assertBugTypeCount("NP_NONNULL_PARAM_VIOLATION", 1);
        assertBugInMethodCount("NP_NONNULL_PARAM_VIOLATION", "TestJSpecifyNonNullEnclosed", "bar", 1);
    }

    @Test
    @Disabled("SpotBugs does not yet support package-level @NullMarked as a nullness default. "
            + "Full support requires implementing scope semantics per https://jspecify.dev/docs/user-guide/#nullmarked")
    void checkedJSpecifyNullMarked() {
        performAnalysis(
                "nullnessAnnotations/jspecify/nullmarked/TestJSpecifyNullMarked.class",
                "org/jspecify/annotations/NullMarked.class");
        assertBugTypeCount("NP_NONNULL_PARAM_VIOLATION", 1);
        assertBugInMethodCount("NP_NONNULL_PARAM_VIOLATION", "TestJSpecifyNullMarked", "bar", 1);
    }
}
