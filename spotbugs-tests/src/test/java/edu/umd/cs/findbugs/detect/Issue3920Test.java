package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

/**
 * Test for GitHub issue #3920:
 * RCN false negative when non-null value is on the left side of null comparison.
 *
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/3920">GitHub issue</a>
 */
class Issue3920Test extends AbstractIntegrationTest {

    @Test
    void testNonNullOnLeftSideOfNullComparison() {
        performAnalysis("ghIssues/Issue3920.class");

        // m1(): System.out == null (IFNONNULL) — previously a false negative
        assertBugInMethod("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE", "Issue3920", "m1");

        // m2(): null == System.out (IF_ACMPNE) — was already detected
        assertBugInMethod("RCN_REDUNDANT_COMPARISON_OF_NULL_AND_NONNULL_VALUE", "Issue3920", "m2");
    }
}
