package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue4296Test extends AbstractIntegrationTest {

    @Test
    void testKnownInstanceofNoFalsePositive() {
        performAnalysis("ghIssues/Issue4296.class");

        // Exactly one warning: the unknown-type parameter case
        assertBugTypeCount("NP_NULL_ON_SOME_PATH", 1);

        // value = new Derived() guarantees instanceof Derived is always true — no warning
        assertNoBugInMethod("NP_NULL_ON_SOME_PATH", "Issue4296", "knownInstanceof");

        // value is a parameter — instanceof may be false, result may stay null — warning expected
        assertBugInMethod("NP_NULL_ON_SOME_PATH", "Issue4296", "unknownInstanceof");
    }
}
