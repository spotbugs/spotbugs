package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Unit test to reproduce <a href="https://github.com/spotbugs/spotbugs/issues/3918">#3918</a>.
 */
class Issue3918Test extends AbstractIntegrationTest {

    @Test
    void testRethrowOfCaughtRuntimeExceptionIsNotReported() {
        performAnalysis("ghIssues/Issue3918.class");

        assertNoBugInMethod("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", "ghIssues.Issue3918", "rethrowFromCatch");
        assertNoBugInMethod("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", "ghIssues.Issue3918", "<init>");
        assertBugInMethod("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", "ghIssues.Issue3918", "throwNewRuntimeException");
    }
}
