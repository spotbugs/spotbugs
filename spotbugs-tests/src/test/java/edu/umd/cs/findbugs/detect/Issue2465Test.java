package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue2465Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        Assertions.assertDoesNotThrow(() -> performAnalysis("ghIssues/Issue2465.class",
                "ghIssues/Issue2465$Role.class"));
    }
}
