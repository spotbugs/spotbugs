package edu.umd.cs.findbugs.nullness;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

/**
 * Checks the effect of CheckerFramework @NonNull annotation on method return value
 */
class TestCheckerFrameworkTypeAnnotations extends AbstractIntegrationTest {

    @BeforeEach
    void performAnalysis() {
        performAnalysis("type/annotation/CheckerFrameworkTypeAnnotations.class");
    }

    @Test
    void methodReturns() {
        assertBugInClassCount("NP_NONNULL_RETURN_VIOLATION", "CheckerFrameworkTypeAnnotations", 2);
    }

    @Test
    void returningNullOnNonNullMethod() {
        assertBugInMethodAtLine("NP_NONNULL_RETURN_VIOLATION", "CheckerFrameworkTypeAnnotations", "returningNullOnNonNullMethod", 13);
    }

    @Test
    void returningNullWithNonNullOnGenerics() {
        assertNoBugInMethod("NP_NONNULL_RETURN_VIOLATION", "CheckerFrameworkTypeAnnotations", "returningNullWithNonNullOnGenerics");
    }

    @Test
    void returningNullOnNonNullMethodWithNonNullOnGenerics() {
        assertBugInMethodAtLine("NP_NONNULL_RETURN_VIOLATION", "CheckerFrameworkTypeAnnotations", "returningNullOnNonNullMethodWithNonNullOnGenerics",
                25);
    }

    @Test
    void methodParametersTests() {
        assertBugInClassCount("NP_NONNULL_PARAM_VIOLATION", "CheckerFrameworkTypeAnnotations", 2);
    }

    @Test
    void usingNullForNonNullParameter() {
        assertBugInMethodAtLine("NP_NONNULL_PARAM_VIOLATION", "CheckerFrameworkTypeAnnotations", "usingNullForNonNullParameter", 30);
    }

    @Test
    void usingNullParameterWithNonNullOnGenerics() {
        assertNoBugInMethod("NP_NONNULL_PARAM_VIOLATION", "CheckerFrameworkTypeAnnotations", "usingNullParameterWithNonNullOnGenerics");
    }

    @Test
    void usingNullOnNonNullParameterWithNonNullOnGenerics() {
        assertBugInMethodAtLine("NP_NONNULL_PARAM_VIOLATION", "CheckerFrameworkTypeAnnotations", "usingNullOnNonNullParameterWithNonNullOnGenerics",
                47);
    }

    @Test
    void usingNonNullArrayOfNullable() {
        assertNoBugInMethod("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "CheckerFrameworkTypeAnnotations", "usingNonNullArrayOfNullable");

    }
}
