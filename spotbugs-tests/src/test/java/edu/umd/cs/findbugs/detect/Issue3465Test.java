package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3465Test extends AbstractIntegrationTest {

    @Test
    void testMutableArrayLiteralInitializer() {
        performAnalysis("ghIssues/Issue3465.class");

        assertBugInClass("MS_PKGPROTECT", "ghIssues.Issue3465");
    }
}
