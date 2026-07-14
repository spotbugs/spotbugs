package edu.umd.cs.findbugs.nullness;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3324Test extends AbstractIntegrationTest {
    /**
     * DummyImpl overrides 3 methods.
     * These 3 methods are all declared to have a non-null return value either via explicit @Nonnull annotation or via
     * the @NonNullByDefault defined in the package-info.java.
     * However, while DummyImpl overrides the 3 methods, all return types are annotated with @Nullable. SpotBugs only
     * detects the 1 violation that uses @Nonnull, but not the other 2 violates that are caused by @NonNullByDefault.
     */
    @Test
    void testIssue() {
        performAnalysis(
                "ghIssues/issue3324/package-info.class",
                "ghIssues/issue3324/DummyAbs.class",
                "ghIssues/issue3324/DummyInterface.class",
                "ghIssues/issue3324/DummyImpl.class");

        assertBugInMethod("NP_METHOD_RETURN_RELAXING_ANNOTATION", "DummyImpl", "getNameDefaultNonnull");
        assertBugInMethod("NP_METHOD_RETURN_RELAXING_ANNOTATION", "DummyImpl", "getNameExplicitNonnullFromInterface");
        assertBugInMethod("NP_METHOD_RETURN_RELAXING_ANNOTATION", "DummyImpl", "getNameDefaultNonnullFromAbs");
        assertBugTypeCount("NP_METHOD_RETURN_RELAXING_ANNOTATION", 3);
    }
}
