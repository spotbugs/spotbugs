package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class Issue4273Test extends AbstractIntegrationTest {

    @Test
    void testKnownInstanceofNoFalsePositive() {
        performAnalysis("ghIssues/Issue4273.class");

        // Only the unknown-type parameter case should warn
        assertBugTypeCount("NP_NULL_ON_SOME_PATH", 1);

        // value = new Derived() guarantees instanceof is always true — no warning
        assertNoBugInMethod("NP_NULL_ON_SOME_PATH", "Issue4273", "knownInstanceof");

        // Negated instanceof is always false — body never executes — no warning
        assertNoBugInMethod("NP_NULL_ON_SOME_PATH", "Issue4273", "knownNegatedInstanceof");

        // value is a parameter — instanceof may be false, result may stay null — warning expected
        assertBugInMethod("NP_NULL_ON_SOME_PATH", "Issue4273", "unknownInstanceof");
    }
}
