package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class Issue2640Test extends AbstractIntegrationTest {
    @Test
    void testIssue() {
        Assertions.assertDoesNotThrow(() -> performAnalysis("ghIssues/Issue2640.class"));
    }
}
