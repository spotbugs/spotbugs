package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue3633Test extends AbstractIntegrationTest {

    @Test
    void reportsConstantDbPasswordForStaticInitializerField() {
        performAnalysis("ghIssues/Issue3633.class");
        assertBugInMethod("DMI_CONSTANT_DB_PASSWORD", "ghIssues.Issue3633", "connectWithStaticBlock");
        assertBugInMethod("DMI_CONSTANT_DB_PASSWORD", "ghIssues.Issue3633", "connectWithInlineConstant");
    }
}
