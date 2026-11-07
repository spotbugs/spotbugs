package edu.umd.cs.findbugs.nullness;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue3335Test extends AbstractIntegrationTest {
    @Test
    void testIssue() {
        final String pkg = "ghIssues/issue3335/";
        performAnalysis(
                pkg + "DummyInterface.class",
                pkg + "DummyImpl.class",
                pkg + "package-info.class");

        assertBugInMethodAtField("NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION", "DummyImpl", "setNullable1", "objRef");
        assertBugInMethodAtField("NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION", "DummyImpl", "setNullable2", "objRef");
    }
}
