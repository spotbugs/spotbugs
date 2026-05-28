package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue4055Test extends AbstractIntegrationTest {

    @Test
    void testLocalClassMethodsReferencedByMethodHandlesAreCallable() {
        performAnalysis("ghIssues/Issue4055.class", "ghIssues/Issue4055$1Accumulator.class");

        assertNoBugType("UMAC_UNCALLABLE_METHOD_OF_ANONYMOUS_CLASS");
    }
}
