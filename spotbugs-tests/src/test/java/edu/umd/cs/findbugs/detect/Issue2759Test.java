package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;


/**
 * @author gtoison
 *
 */
class Issue2759Test extends AbstractIntegrationTest {
    @Test
    void testIssue() {
        Assertions.assertDoesNotThrow(() -> performAnalysis(
                "../../../../src/classSamples/classesModifiedByJacoco/JavaLanguageParser$ClassBlockContext.class"));
    }
}
