package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue1147Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue1147.class");

        assertBugTypeCount("DMI_INVOKING_TOSTRING_ON_ARRAY", 1);
    }
}
