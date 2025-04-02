package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3305Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("../../../../src/classSamples/ecjSwitchTable/SwitchTableBug.class",
                "../../../../src/classSamples/ecjSwitchTable/SwitchTableBug$A.class",
                "../../../../src/classSamples/ecjSwitchTable/SwitchTableBug$B.class",
                "../../../../src/classSamples/ecjSwitchTable/SwitchTableBug$X.class");

        assertNoBugType("HSM_HIDING_METHOD");
    }
}
