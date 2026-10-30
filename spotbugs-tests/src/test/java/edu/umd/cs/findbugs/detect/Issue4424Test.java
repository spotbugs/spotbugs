package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue4424Test extends AbstractIntegrationTest {

    @Test
    void ignoresNullnessAnnotationsOnTypeArguments() {
        performAnalysis("ghIssues/Issue4424.class",
                "ghIssues/Issue4424$HasListener.class",
                "ghIssues/Issue4424$SpecificHasListener.class",
                "ghIssues/Issue4424$TighteningHasListener.class");

        assertNoBugInClass("NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION", "Issue4424$SpecificHasListener");
        assertNoBugInClass("NP_METHOD_RETURN_RELAXING_ANNOTATION", "Issue4424$SpecificHasListener");

        assertBugTypeCount("NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION", 1);
        assertBugInMethod("NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION", "Issue4424$TighteningHasListener", "setListener");
        assertBugTypeCount("NP_METHOD_RETURN_RELAXING_ANNOTATION", 1);
        assertBugInMethod("NP_METHOD_RETURN_RELAXING_ANNOTATION", "Issue4424$TighteningHasListener", "getItems");
    }
}
