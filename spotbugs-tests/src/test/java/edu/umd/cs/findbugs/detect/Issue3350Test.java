package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3350Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("../../../../src/classSamples/lombokAddSuppressFBWarnings/DemoApplication.class",
                "../../../../src/classSamples/lombokAddSuppressFBWarnings/DemoApplication$BrokenClass.class");

        assertNoBugType("US_USELESS_SUPPRESSION_ON_METHOD");
    }
}
