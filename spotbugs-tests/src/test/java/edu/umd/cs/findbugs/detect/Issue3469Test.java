package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3469Test extends AbstractIntegrationTest {

    @Test
    void testRecursiveLoopWithConstantBooleanHelper() {
        performAnalysis("ghIssues/Issue3469.class");

        assertBugInMethod("IL_INFINITE_RECURSIVE_LOOP", "ghIssues.Issue3469", "infiniteLoopMethod");
    }
}
