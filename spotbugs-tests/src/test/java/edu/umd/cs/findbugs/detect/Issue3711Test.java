package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class Issue3711Test extends AbstractIntegrationTest {

    @Test
    void test() {
        Assertions.assertDoesNotThrow(() -> performAnalysis("ghIssues/Issue3711.class"));
    }
}
