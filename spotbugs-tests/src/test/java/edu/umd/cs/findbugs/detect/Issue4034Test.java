package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue4034Test extends AbstractIntegrationTest {

    @Test
    void testUwfNullFieldWithCastNullInitializer() {
        performAnalysis("ghIssues/Issue4034.class");

        assertBugAtField("UWF_NULL_FIELD", "ghIssues.Issue4034", "strVal");
    }
}
