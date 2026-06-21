package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3456Test extends AbstractIntegrationTest {

    @Test
    void testBitAndWithTemporary() {
        performAnalysis("ghIssues/Issue3456.class");

        assertBugInMethod("BIT_AND", "ghIssues.Issue3456", "showBugWithTemporary");
        assertBugInMethod("BIT_AND", "ghIssues.Issue3456", "showBugDirect");
        assertBugInMethod("BIT_AND_ZZ", "ghIssues.Issue3456", "showBugZeroMaskWithTemporary");
    }
}
