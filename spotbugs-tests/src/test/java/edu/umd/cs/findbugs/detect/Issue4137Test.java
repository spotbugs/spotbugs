package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue4137Test extends AbstractIntegrationTest {
    private static final String OPS_BUG = "AT_NONATOMIC_OPERATIONS_ON_SHARED_VARIABLE";

    @Test
    void testInnerClassRmwOfEnclosingSharedFieldIsReported() {
        performAnalysis("ghIssues/Issue4137.class", "ghIssues/Issue4137$SubAdditionOnSharedVariable.class");
        // Baseline: the enclosing Thread subclass itself is reported for its
        // own read-modify-write of the shared field.
        assertBugInMethod(OPS_BUG, "ghIssues.Issue4137", "toggle");
        // Precise case from the issue: a non-static inner class performs a
        // non-atomic read-modify-write of a field owned by its enclosing
        // (multi-threaded) class via the synthetic this$0 reference. After the
        // fix this is reported, not silently dropped.
        assertBugInMethod(OPS_BUG, "ghIssues.Issue4137$SubAdditionOnSharedVariable", "toggle2");
        assertBugTypeCount(OPS_BUG, 2);
    }
}
