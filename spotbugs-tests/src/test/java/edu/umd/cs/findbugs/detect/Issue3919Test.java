package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Unit test to reproduce <a href="https://github.com/spotbugs/spotbugs/issues/3919">#3919</a>.
 */
class Issue3919Test extends AbstractIntegrationTest {

    @Test
    void testRuntimeExceptionFromMethodReturnValueIsNotReported() {
        performAnalysis("ghIssues/Issue3919.class");

        assertNoBugInMethod("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", "ghIssues.Issue3919", "throwViaHelper");
        assertNoBugInMethod("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", "ghIssues.Issue3919", "throwInlineSubtypes");
        assertBugInMethod("THROWS_METHOD_THROWS_RUNTIMEEXCEPTION", "ghIssues.Issue3919", "throwDirectRuntimeException");
    }
}
