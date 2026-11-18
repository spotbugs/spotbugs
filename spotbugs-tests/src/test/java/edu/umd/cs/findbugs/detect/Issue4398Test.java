package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue4398Test extends AbstractIntegrationTest {
    private static final String OPS_BUG = "AT_NONATOMIC_OPERATIONS_ON_SHARED_VARIABLE";
    private static final String BIT64_BUG = "AT_NONATOMIC_64BIT_PRIMITIVE";
    private static final String STALE_WRITE_BUG = "AT_STALE_THREAD_WRITE_OF_PRIMITIVE";

    @Test
    void testOwnFieldsOfInnerClassesAreNotReported() {
        performAnalysis("ghIssues/Issue4398.class", "ghIssues/Issue4398$1.class", "ghIssues/Issue4398$PatternCursor.class");
        assertBugTypeCount(OPS_BUG, 0);
        assertBugTypeCount(BIT64_BUG, 0);
        assertBugTypeCount(STALE_WRITE_BUG, 0);
    }

    @Test
    void testWritesOfMultiThreadedInnerClassesAreReported() {
        performAnalysis("ghIssues/Issue4398.class", "ghIssues/Issue4398$Session.class", "ghIssues/Issue4398$Session$Closer.class",
                "ghIssues/Issue4398$Heartbeat.class", "ghIssues/Issue4398$Heartbeat$Pinger.class");
        assertBugTypeCount(OPS_BUG, 0);
        assertBugTypeCount(BIT64_BUG, 0);
        assertBugTypeCount(STALE_WRITE_BUG, 2);
        assertBugInMethod(STALE_WRITE_BUG, "ghIssues.Issue4398$Session$Closer", "run");
        assertBugInMethod(STALE_WRITE_BUG, "ghIssues.Issue4398$Heartbeat$Pinger", "stop");
    }
}
