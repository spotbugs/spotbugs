package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue2965Test extends AbstractIntegrationTest {

    @Test
    void testIssue() {
        performAnalysis("ghIssues/Issue2965.class");

        assertNoBugInMethod("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "ghIssues.Issue2965", "checkForNullRequireNonNull");
        assertNoBugInMethod("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "ghIssues.Issue2965", "checkForNullRequireNonNullWithMessage");
        assertNoBugInMethod("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "ghIssues.Issue2965", "checkForNullRequireNonNullWithMessageMultiLine");
        assertNoBugInMethod("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "ghIssues.Issue2965", "checkForNullRequireNonNullWithMessageSupplier");
        assertNoBugInMethod("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "ghIssues.Issue2965", "conditionalCheckForNull");
        assertNoBugInMethod("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "ghIssues.Issue2965", "springNullableRequireNonNull");
        assertNoBugInMethod("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "ghIssues.Issue2965", "checkForNullRequireNonNullChainedCall");

        // Check that we can still detect a load of null
        assertBugInMethod("NP_LOAD_OF_KNOWN_NULL_VALUE", "ghIssues.Issue2965", "nullRequireNonNull");
        assertBugInMethod("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", "ghIssues.Issue2965", "requireNonNullDereference");

        assertBugTypeCount("NP_LOAD_OF_KNOWN_NULL_VALUE", 1);
        assertBugTypeCount("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE", 1);
    }
}
