package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.condition.EnabledForJreRange;
import org.junit.jupiter.api.condition.JRE;

class Issue4409Test extends AbstractIntegrationTest {

    @Test
    @EnabledForJreRange(min = JRE.JAVA_25)
    void testIssue() {
        performAnalysis(
                "../java25/Issue4409.class",
                "../java25/Issue4409$Foo.class");

        assertBugTypeCount("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", 0);
    }
}
