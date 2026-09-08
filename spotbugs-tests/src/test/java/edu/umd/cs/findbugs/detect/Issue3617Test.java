package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3617Test extends AbstractIntegrationTest {

    @Test
    void testFallthrough() {
        performAnalysis("ghIssues/Issue3617.class");

        assertNoBugType("SF_SWITCH_FALLTHROUGH");
    }
}
