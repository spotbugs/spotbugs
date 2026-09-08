/**
 *
 */
package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

/**
 *
 */
class LombokWithTest extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("../../../../src/classSamples/lombokRecordWith/Instance.class");

        assertNoBugType("RC_REF_COMPARISON");
    }
}
