package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue1043Test extends AbstractIntegrationTest {

    @Test
    void testUnwrittenFieldWithClassLiteralAccess() {
        performAnalysis("ghIssues/Issue1043.class");
        assertBugAtLine("UWF_UNWRITTEN_FIELD", 13);
    }
}
