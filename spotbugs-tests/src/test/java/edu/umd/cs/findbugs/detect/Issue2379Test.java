package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue2379Test extends AbstractIntegrationTest {
    @Test
    void test() {
        performAnalysis("ghIssues/Issue2379.class");

        // Check that we've found the uncalled `unusedValues` private method
        assertBugInMethod("UPM_UNCALLED_PRIVATE_METHOD", "Issue2379", "unusedValues");

        // Check that it was the one and only bug we've found
        assertBugTypeCount("UPM_UNCALLED_PRIVATE_METHOD", 1);
    }
}
