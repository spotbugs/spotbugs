package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3390Test extends AbstractIntegrationTest {
    @Test
    void testNotThreadSafe() {
        performAnalysis("ghIssues/Issue3390NotThreadSafe.class");
        assertBugInClassCount("AT_NONATOMIC_OPERATIONS_ON_SHARED_VARIABLE", "ghIssues.Issue3390NotThreadSafe", 0);
    }
}
