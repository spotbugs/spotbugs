package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Unit test to reproduce <a href="https://github.com/spotbugs/spotbugs/issues/3900">#3900</a>.
 */
class Issue3900Test extends AbstractIntegrationTest {

    @Test
    void testIgnoredArraysCopyOfReturnValue() {
        performAnalysis("ghIssues/Issue3900.class");

        assertBugInMethod("RV_RETURN_VALUE_IGNORED_NO_SIDE_EFFECT", "ghIssues.Issue3900", "ignoredCopyOf");
    }
}
