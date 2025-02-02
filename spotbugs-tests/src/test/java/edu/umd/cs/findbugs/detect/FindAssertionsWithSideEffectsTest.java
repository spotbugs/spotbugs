package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

class FindAssertionsWithSideEffectsTest extends AbstractIntegrationTest {

    @Test
    void testArgumentAssertions() {
        performAnalysis("AssertionsWithSideEffects.class",
                "AssertionsWithSideEffects$ImmutableList.class");

        assertBugTypeCount("ASE_ASSERTION_WITH_SIDE_EFFECT", 3);
        assertBugTypeCount("ASE_ASSERTION_WITH_SIDE_EFFECT_METHOD", 1);

        assertBugInMethodAtLine("ASE_ASSERTION_WITH_SIDE_EFFECT", "AssertionsWithSideEffects", "iinc", 6);
        assertBugInMethodAtLine("ASE_ASSERTION_WITH_SIDE_EFFECT", "AssertionsWithSideEffects", "storeBoolean", 12);
        assertBugInMethodAtLine("ASE_ASSERTION_WITH_SIDE_EFFECT", "AssertionsWithSideEffects", "storeInt", 18);
        assertBugInMethodAtLine("ASE_ASSERTION_WITH_SIDE_EFFECT_METHOD", "AssertionsWithSideEffects", "addAndRemove", 26);
    }
}
