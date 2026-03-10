package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3169Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue3169.class");

        assertNoBugType("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR");
    }
}
