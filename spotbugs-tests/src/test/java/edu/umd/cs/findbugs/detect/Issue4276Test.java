package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue4276Test extends AbstractIntegrationTest {
    @Test
    void testLazyInitStaticWithMethodCall() {
        performAnalysis("ghIssues/Issue4276.class");
        assertBugTypeCount("LI_LAZY_INIT_STATIC", 1);
        assertBugInMethod("LI_LAZY_INIT_STATIC", "Issue4276", "test");
    }
}
