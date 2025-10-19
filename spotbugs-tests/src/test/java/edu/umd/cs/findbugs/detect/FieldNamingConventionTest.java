package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class FieldNamingConventionTest extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("badNaming/FieldNamingConvention.class");
        assertBugTypeCount("NM_FIELD_NAMING_CONVENTION", 1);
    }
}
