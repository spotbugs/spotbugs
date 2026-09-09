package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class Issue4154Test extends AbstractIntegrationTest {

    @Test
    void testTernaryLazyInitReportsUnsynchronizedGetter() {
        performAnalysis("ghIssues/Issue4154.class");

        assertNoBugInClass("SING_SINGLETON_IMPLEMENTS_CLONEABLE", "Issue4154");
        assertBugInClassCount("SING_SINGLETON_GETTER_NOT_SYNCHRONIZED", "Issue4154", 1);
    }
}
