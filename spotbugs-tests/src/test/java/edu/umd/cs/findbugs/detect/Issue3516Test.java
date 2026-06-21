package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3516Test extends AbstractIntegrationTest {

    @Test
    void noAtomicityWarningWhenLockIsAcquiredViaAutoCloseableWrapper() {
        performAnalysis("ghIssues/Issue3516.class");
        assertBugInClassCount("AT_NONATOMIC_OPERATIONS_ON_SHARED_VARIABLE", "ghIssues.Issue3516", 0);
        assertBugInClassCount("AT_STALE_THREAD_WRITE_OF_PRIMITIVE", "ghIssues.Issue3516", 0);
    }
}
