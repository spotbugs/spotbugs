package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue1641Test extends AbstractIntegrationTest {

    @Test
    void reportsInfiniteRecursionWhenBothBranchesRecurse() {
        performAnalysis("ghIssues/Issue1641.class");
        assertBugInMethodCount("IL_INFINITE_RECURSIVE_LOOP", "ghIssues.Issue1641", "methodA", 2);
    }
}
