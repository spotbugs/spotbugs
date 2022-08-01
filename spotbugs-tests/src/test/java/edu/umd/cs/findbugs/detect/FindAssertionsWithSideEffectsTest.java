package edu.umd.cs.findbugs.detect;

import static edu.umd.cs.findbugs.test.CountMatcher.containsExactly;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.annotations.Confidence;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcher;
import edu.umd.cs.findbugs.test.matcher.BugInstanceMatcherBuilder;

public class FindAssertionsWithSideEffectsTest extends AbstractIntegrationTest {
    @Test
    public void testArgumentAssertions() {
        performAnalysis("AssertionsWithSideEffects.class",
                "AssertionsWithSideEffects$ImmutableList.class");

        assertNumOfBugs("ASE_ASSERTION_WITH_SIDE_EFFECT", 3);
        assertNumOfBugs("ASE_ASSERTION_WITH_SIDE_EFFECT_METHOD", 1);

        assertBug("ASE_ASSERTION_WITH_SIDE_EFFECT", "iinc", 6);
        assertBug("ASE_ASSERTION_WITH_SIDE_EFFECT", "storeBoolean", 12);
        assertBug("ASE_ASSERTION_WITH_SIDE_EFFECT", "storeInt", 18);
        assertBug("ASE_ASSERTION_WITH_SIDE_EFFECT_METHOD", "addAndRemove", 26);
    }

    private void assertNumOfBugs(String error, int num) {
        final BugInstanceMatcher bugTypeMatcher = new BugInstanceMatcherBuilder()
                .bugType(error).build();
        assertThat(getBugCollection(), containsExactly(num, bugTypeMatcher));
    }

    private void assertBug(String error, String method, int line) {
        final BugInstanceMatcher bugInstanceMatcher = new BugInstanceMatcherBuilder()
                .bugType(error)
                .inClass("AssertionsWithSideEffects")
                .inMethod(method)
                .atLine(line)
                .withConfidence(Confidence.LOW)
                .build();
        assertThat(getBugCollection(), hasItem(bugInstanceMatcher));
    }
}
