package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue4137Test extends AbstractIntegrationTest {
    private static final String OPS_BUG = "AT_NONATOMIC_OPERATIONS_ON_SHARED_VARIABLE";
    private static final String BIT64_BUG = "AT_NONATOMIC_64BIT_PRIMITIVE";
    private static final String STALE_WRITE_BUG = "AT_STALE_THREAD_WRITE_OF_PRIMITIVE";

    @Test
    void testInnerClassRmwOfEnclosingSharedFieldIsReported() {
        performAnalysis("ghIssues/Issue4137.class", "ghIssues/Issue4137$SubAdditionOnSharedVariable.class",
                "ghIssues/Issue4137$StaticNested.class");
        // Baseline: the enclosing Thread subclass itself is reported for its
        // own read-modify-write of the shared field.
        assertBugInMethod(OPS_BUG, "ghIssues.Issue4137", "toggle");
        // Precise case from the issue: a non-static inner class performs a
        // non-atomic read-modify-write of a field owned by its enclosing
        // (multi-threaded) class via the synthetic this$0 reference. After the
        // fix this is reported, not silently dropped.
        assertBugInMethod(OPS_BUG, "ghIssues.Issue4137$SubAdditionOnSharedVariable", "toggle2");
        // A static nested class has no enclosing instance, so it does not
        // inherit the enclosing Thread's multi-threaded context. Its own
        // non-atomic read-modify-write (same shape, read in getOwn()) must not
        // be reported. The count stays at 2; without the static-nesting guard
        // StaticNested.toggle3 would be reported too, raising it to 3.
        assertBugTypeCount(OPS_BUG, 2);
        // The corpus only exercises the non-atomic RMW shape on int fields, so
        // the detector's other two bug types must not be reported on it.
        assertBugTypeCount(BIT64_BUG, 0);
        assertBugTypeCount(STALE_WRITE_BUG, 0);
    }
}
