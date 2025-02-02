package edu.umd.cs.findbugs.ba;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue893Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        Assertions.assertDoesNotThrow(() -> performAnalysis("ghIssues/Issue893.class"));
    }

}
