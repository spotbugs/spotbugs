package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.annotations.Confidence;
import org.apache.bcel.Const;
import org.junit.jupiter.api.Test;

class Issue2837Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue2837.class",
                "ghIssues/Issue2837$InvokedNegation.class",
                "ghIssues/Issue2837$EagerFactory.class",
                "ghIssues/Issue2837$OverriddenNegation.class",
                "ghIssues/Issue2837$OverriddenNegation$EagerPredicate.class");

        assertBugTypeCount("MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR", 4);
        assertBugInMethodAtLineWithConfidence("MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR", "Issue2837",
                Const.CONSTRUCTOR_NAME, 19, Confidence.LOW);
        assertBugInMethodAtLineWithConfidence("MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR", "Issue2837$InvokedNegation",
                Const.CONSTRUCTOR_NAME, 31, Confidence.LOW);
        assertBugInMethodAtLineWithConfidence("MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR", "Issue2837$EagerFactory",
                Const.CONSTRUCTOR_NAME, 44, Confidence.LOW);
        assertBugInMethodAtLineWithConfidence("MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR", "Issue2837$OverriddenNegation",
                Const.CONSTRUCTOR_NAME, 60, Confidence.LOW);
    }
}
