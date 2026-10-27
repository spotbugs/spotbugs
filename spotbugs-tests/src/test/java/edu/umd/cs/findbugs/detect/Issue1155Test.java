package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/1155">GitHub issue</a>
 */
class Issue1155Test extends AbstractIntegrationTest {
    @Test
    void testIssue1155() {
        performAnalysis("ghIssues/Issue1155.class");

        // Issue 1155 is about getting a false positive on the NP_NULL_ON_SOME_PATH
        assertNoBugType("NP_NULL_ON_SOME_PATH");

        // Verify that none of the other possible issues that can be reported by
        // edu.umd.cs.findbugs.detect.FindNullDeref are present.
        final String[] findNullDerefBugTypes = new String[] {
            "NP_DEREFERENCE_OF_READLINE_VALUE",
            "NP_NULL_ON_SOME_PATH_MIGHT_BE_INFEASIBLE",
            "NP_TOSTRING_COULD_RETURN_NULL",
            "NP_CLONE_COULD_RETURN_NULL",
            "NP_ALWAYS_NULL_EXCEPTION",
            "NP_ALWAYS_NULL",
            "NP_STORE_INTO_NONNULL_FIELD",
            "NP_NULL_ON_SOME_PATH_EXCEPTION",
            "NP_NULL_ON_SOME_PATH",
            "NP_NULL_PARAM_DEREF_NONVIRTUAL",
            "NP_NULL_PARAM_DEREF_ALL_TARGETS_DANGEROUS",
            "NP_NULL_PARAM_DEREF",
            "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
            "RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE",
            "RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE",
            "RCN_REDUNDANT_COMPARISON_TWO_NULL_VALUES",
            "RCN_REDUNDANT_COMPARISON_OF_NULL_AND_NONNULL_VALUE",
            "NP_NONNULL_PARAM_VIOLATION",
            "NP_NONNULL_RETURN_VIOLATION",
            "NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE",
            "NP_GUARANTEED_DEREF",
            "NP_GUARANTEED_DEREF_ON_EXCEPTION_PATH",
            "NP_EQUALS_SHOULD_HANDLE_NULL_ARGUMENT",
            "NP_ARGUMENT_MIGHT_BE_NULL",
            "NP_CLOSING_NULL"
        };
        for (String findNullDerefBugType : findNullDerefBugTypes) {
            assertNoBugType(findNullDerefBugType);
        }
    }
}
