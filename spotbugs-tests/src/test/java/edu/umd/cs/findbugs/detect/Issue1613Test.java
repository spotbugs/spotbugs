package edu.umd.cs.findbugs.detect;


import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue1613Test extends AbstractIntegrationTest {

    @Test
    void suppressed() {
        performAnalysis("ghIssues/issue1613/Issue1613Suppressed.class");

        assertBugTypeCount("DMI_RANDOM_USED_ONLY_ONCE", 0);
    }

    @Test
    void notSuppressed() {
        performAnalysis("ghIssues/issue1613/Issue1613NotSuppressed.class");

        assertBugTypeCount("DMI_RANDOM_USED_ONLY_ONCE", 1);
    }
}
