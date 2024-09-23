package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue453Test extends AbstractIntegrationTest {
    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue453.class");
        assertNoBugType("UWF_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR");
    }
}
