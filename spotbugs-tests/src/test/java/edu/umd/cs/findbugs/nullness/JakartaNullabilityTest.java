package edu.umd.cs.findbugs.nullness;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Check if Nullable/Nonnull annotation from jakarta.annotations is detected.
 *
 * @author pkini07
 */
class JakartaNullabilityTest extends AbstractIntegrationTest {

    @Test
    void checkedJakartaNullableReturn_isOk() {
        performAnalysis("nullnessAnnotations/CheckedJakartaNullableReturn.class", "jakarta/annotation/Nullable.class");
        assertNoBugType("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE");
    }

    @Test
    void uncheckedJakartaNullableReturn_isDetected() {
        performAnalysis("nullnessAnnotations/UncheckedJakartaNullableReturn.class", "jakarta/annotation/Nullable.class");
        assertBugTypeCount("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", 1);
    }

    @Test
    void checkedJakartaNonnullReturn_isOk() {
        performAnalysis("nullnessAnnotations/CheckedJakartaNonnullReturn.class", "jakarta/annotation/Nonnull.class");
        assertNoBugType("NP_NONNULL_RETURN_VIOLATION");
    }

    @Test
    void uncheckedJakartaNonnullReturn_isDetected() {
        performAnalysis("nullnessAnnotations/UncheckedJakartaNonnullReturn.class", "jakarta/annotation/Nonnull.class");
        assertBugTypeCount("NP_NONNULL_RETURN_VIOLATION", 1);
    }
}
