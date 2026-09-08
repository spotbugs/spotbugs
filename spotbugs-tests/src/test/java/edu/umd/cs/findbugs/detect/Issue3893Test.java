package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3893Test extends AbstractIntegrationTest {

    @Test
    void testStWriteToStaticFromInnerClass() {
        performAnalysis("ghIssues/Issue3893.class", "ghIssues/Issue3893$Inner.class");

        // True positive (always worked): outer class writes to static field
        assertBugInMethod("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", "ghIssues.Issue3893", "setCounter");

        // False negative (now fixed): named non-static inner class writes to outer static field
        assertBugInMethod("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", "ghIssues.Issue3893$Inner", "setCounter");

        // False negative (now fixed): named non-static inner class writes via explicit class qualifier
        assertBugInMethod("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD", "ghIssues.Issue3893$Inner", "resetCounter");
    }
}
