package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3668Test extends AbstractIntegrationTest {

    @Test
    void testNakedNotifyAfterConditional() {
        performAnalysis("ghIssues/Issue3668.class");
        assertBugInMethod("NN_NAKED_NOTIFY", "ghIssues.Issue3668", "foo");
        assertNoBugInMethod("NN_NAKED_NOTIFY", "ghIssues.Issue3668", "fooWithMutation");
    }
}
