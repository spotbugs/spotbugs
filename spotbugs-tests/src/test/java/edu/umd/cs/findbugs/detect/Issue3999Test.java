package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue3999Test extends AbstractIntegrationTest {

    @Test
    void testNoTqInGeneratedClass() {
        performAnalysis("ghIssues/issue3999/Issue3999ClassGenerated.class", "ghIssues/issue3999/Generated.class");

        assertNoBugType("TQ_UNKNOWN_VALUE_USED_WHERE_ALWAYS_STRICTLY_REQUIRED");
    }

    @Test
    void testNoTqInGeneratedMethod() {
        performAnalysis("ghIssues/issue3999/Issue3999MethodGenerated.class", "ghIssues/issue3999/Generated.class");

        assertNoBugType("TQ_UNKNOWN_VALUE_USED_WHERE_ALWAYS_STRICTLY_REQUIRED");
    }
}
