package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3530Test extends AbstractIntegrationTest {

    @Test
    void testFallthrough() {
        performAnalysis("ghIssues/Issue3530.class");

        assertBugInMethod("SF_DEAD_STORE_DUE_TO_SWITCH_FALLTHROUGH", "ghIssues.Issue3530", "switchForLoop");
        assertBugInMethod("SF_SWITCH_FALLTHROUGH", "ghIssues.Issue3530", "switchForLoop");

        assertBugInMethod("SF_DEAD_STORE_DUE_TO_SWITCH_FALLTHROUGH_TO_THROW", "ghIssues.Issue3530", "switchWhileLoop");
        assertBugInMethod("SF_SWITCH_FALLTHROUGH", "ghIssues.Issue3530", "switchWhileLoop");
    }
}
