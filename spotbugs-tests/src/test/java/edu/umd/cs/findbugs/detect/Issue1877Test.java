package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue1877Test extends AbstractIntegrationTest {

    @Test
    @EnabledForJreRange(min = JRE.JAVA_17)
    void testIssue() {
        performAnalysis("../java17/Issue1877.class");

        assertBugTypeCount("VA_FORMAT_STRING_USES_NEWLINE", 2);

        assertBugAtLine("VA_FORMAT_STRING_USES_NEWLINE", 18);
        assertBugAtLine("VA_FORMAT_STRING_USES_NEWLINE", 37);
    }
}
