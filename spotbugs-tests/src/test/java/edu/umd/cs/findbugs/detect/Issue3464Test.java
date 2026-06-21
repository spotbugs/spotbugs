package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3464Test extends AbstractIntegrationTest {

    @Test
    void testMutableStaticStringBuilder() {
        performAnalysis("ghIssues/Issue3464.class");

        assertBugInClass("MS_SHOULD_BE_FINAL", "ghIssues.Issue3464");
    }
}
