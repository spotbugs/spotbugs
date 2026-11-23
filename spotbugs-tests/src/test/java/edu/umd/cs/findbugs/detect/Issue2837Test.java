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
                Const.CONSTRUCTOR_NAME, 22, Confidence.LOW);
        assertBugInMethodAtLineWithConfidence("MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR", "Issue2837$InvokedNegation",
                Const.CONSTRUCTOR_NAME, 34, Confidence.LOW);
        assertBugInMethodAtLineWithConfidence("MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR", "Issue2837$EagerFactory",
                Const.CONSTRUCTOR_NAME, 47, Confidence.LOW);
        assertBugInMethodAtLineWithConfidence("MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR", "Issue2837$OverriddenNegation",
                Const.CONSTRUCTOR_NAME, 63, Confidence.LOW);
    }

    @Test
    void tracksCallbackCombinationsUntilTheyAreStoredOrUsed() {
        performAnalysis("ghIssues/Issue2837$DeferredCombinators.class",
                "ghIssues/Issue2837$DeferredCombinators$InheritedPredicate.class",
                "ghIssues/Issue2837$StoredThenInvoked.class",
                "ghIssues/Issue2837$UnknownReceiver.class");

        String bug = "MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR";
        assertNoBugInClass(bug, "Issue2837$DeferredCombinators");
        assertBugTypeCount(bug, 5);
        assertBugInMethodAtLineWithConfidence(bug, "Issue2837$StoredThenInvoked", Const.CONSTRUCTOR_NAME, 99, Confidence.LOW);
        assertBugInMethodAtLineWithConfidence(bug, "Issue2837$StoredThenInvoked", Const.CONSTRUCTOR_NAME, 100, Confidence.LOW);
        assertBugInMethodAtLineWithConfidence(bug, "Issue2837$StoredThenInvoked", Const.CONSTRUCTOR_NAME, 101, Confidence.LOW);
        assertBugInMethodAtLineWithConfidence(bug, "Issue2837$StoredThenInvoked", Const.CONSTRUCTOR_NAME, 102, Confidence.LOW);
        assertBugInMethodAtLineWithConfidence(bug, "Issue2837$UnknownReceiver", Const.CONSTRUCTOR_NAME, 131, Confidence.LOW);
    }
}
