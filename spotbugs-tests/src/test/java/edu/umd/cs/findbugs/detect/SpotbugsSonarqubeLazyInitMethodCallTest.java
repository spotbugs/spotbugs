package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Test for LI_LAZY_INIT_STATIC detection
 * This test verifies that SpotBugs correctly detects lazy initialization of static fields
 * without proper synchronization.
 */
class SpotbugsSonarqubeLazyInitMethodCallTest extends AbstractIntegrationTest {

    @Test
    void testLazyInitStatic() {
        performAnalysis("ghIssues/SpotbugsSonarqubeLazyInitMethodCall.class");

        // Expect LI_LAZY_INIT_STATIC to be reported in the test method
        assertBugInMethod("LI_LAZY_INIT_STATIC", "ghIssues.SpotbugsSonarqubeLazyInitMethodCall", "test");
    }
}
