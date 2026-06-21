package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3529Test extends AbstractIntegrationTest {

    @Test
    void testDatabasePasswordFromTernary() {
        performAnalysis("ghIssues/Issue3529.class");

        assertBugInMethod("DMI_CONSTANT_DB_PASSWORD", "ghIssues.Issue3529", "showBugWithTernary");
        assertBugInMethod("DMI_EMPTY_DB_PASSWORD", "ghIssues.Issue3529", "showBugWithTernary");
        assertBugInMethod("DMI_CONSTANT_DB_PASSWORD", "ghIssues.Issue3529", "showBugWithConstantBranch");
    }
}
