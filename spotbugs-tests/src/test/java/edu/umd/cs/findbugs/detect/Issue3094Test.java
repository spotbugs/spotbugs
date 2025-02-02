package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

/**
 * @author gtoison
 *
 */
class Issue3094Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("../../../../src/classSamples/kotlin/DummyBE.class", "kotlin/jvm/internal/Intrinsics.class");
        assertNoBugType("NP_NONNULL_FIELD_NOT_INITIALIZED_IN_CONSTRUCTOR");
    }
}
