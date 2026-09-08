package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue1877Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("../java17/Issue1877.class");

        assertBugTypeCount("VA_FORMAT_STRING_USES_NEWLINE", 2);

        assertBugAtLine("VA_FORMAT_STRING_USES_NEWLINE", 18);
        assertBugAtLine("VA_FORMAT_STRING_USES_NEWLINE", 37);
    }
}
