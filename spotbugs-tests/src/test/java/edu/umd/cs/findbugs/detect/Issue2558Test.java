package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue2558Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("ghIssues/Issue2558.class");
        assertNoBugType("NP_EQUALS_SHOULD_HANDLE_NULL_ARGUMENT");
    }
}
