package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/3483">GitHub issue #3483</a>
 */
class Issue3483Test extends AbstractIntegrationTest {

    /**
     * A method that reads {@code $assertionsDisabled} without ever creating an
     * {@code AssertionError} must not leave the detector "inside an assertion" for the
     * methods scanned afterwards.
     */
    @Test
    void assertionStateDoesNotLeakBetweenMethods() {
        performAnalysis("ghIssues/Issue3483.class");

        assertBugTypeCount("ASE_ASSERTION_WITH_SIDE_EFFECT", 0);
        assertBugTypeCount("ASE_ASSERTION_WITH_SIDE_EFFECT_METHOD", 0);
    }

    /**
     * Detector instances are reused for the whole analysis run, so the state must not leak
     * into the following classes either.
     */
    @Test
    void assertionStateDoesNotLeakBetweenClasses() {
        performAnalysis("ghIssues/Issue3483Poison.class", "ghIssues/Issue3483Victim.class");

        assertBugTypeCount("ASE_ASSERTION_WITH_SIDE_EFFECT", 0);
        assertBugTypeCount("ASE_ASSERTION_WITH_SIDE_EFFECT_METHOD", 0);
    }
}
