package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue4436Test extends AbstractIntegrationTest {

    @Test
    void reportsDuplicateBranchesInNestedIfElse() {
        performAnalysis("ghIssues/Issue4436.class");

        assertBugTypeCount("DB_DUPLICATE_BRANCHES", 2);
        assertBugInMethod("DB_DUPLICATE_BRANCHES", "Issue4436", "seed");
        assertBugInMethod("DB_DUPLICATE_BRANCHES", "Issue4436", "nested");
    }
}
