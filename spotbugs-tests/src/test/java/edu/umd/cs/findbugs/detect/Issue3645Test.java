package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

class Issue3645Test extends AbstractIntegrationTest {

    @Test
    @EnabledForJreRange(min = JRE.JAVA_21)
    void testIssue() {
        performAnalysis(
                "../java21/Issue3645.class",
                "../java21/Issue3645$ProgressSource.class",
                "../java21/Issue3645$SourceProgress.class");

        assertBugTypeCount("SF_SWITCH_FALLTHROUGH", 0);
    }
}
