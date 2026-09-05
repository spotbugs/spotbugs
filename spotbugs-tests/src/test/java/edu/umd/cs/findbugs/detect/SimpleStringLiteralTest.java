package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Test to verify that simple string literal assignments are NOT flagged
 * as LI_LAZY_INIT_STATIC since they are safe due to String immutability.
 */
class SimpleStringLiteralTest extends AbstractIntegrationTest {

    @Test
    void testSimpleStringLiteral() {
        performAnalysis("ghIssues/SimpleStringLiteralTest.class");

        // Should NOT report LI_LAZY_INIT_STATIC for simple string literal
        assertNoBugInMethod("LI_LAZY_INIT_STATIC", "ghIssues.SimpleStringLiteralTest", "test");
    }
}
