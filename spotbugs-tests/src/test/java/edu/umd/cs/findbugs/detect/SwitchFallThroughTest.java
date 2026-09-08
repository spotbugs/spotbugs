package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

class SwitchFallThroughTest extends AbstractIntegrationTest {
    @Test
    void testBug2539590() {
        performAnalysis("sfBugs/Bug2539590.class");

        assertBugInMethod("SF_SWITCH_NO_DEFAULT", "Bug2539590", "noFallthroughMethodNoDefault");
        assertNoBugInMethod("SF_SWITCH_NO_DEFAULT", "Bug2539590", "noFallthroughMethod");
        assertNoBugInMethod("SF_SWITCH_NO_DEFAULT", "Bug2539590", "noFallthroughMethod2");

        assertBugInMethod("SF_SWITCH_NO_DEFAULT", "Bug2539590", "fallthroughMethodNoDefault");
        assertNoBugInMethod("SF_SWITCH_FALLTHROUGH", "Bug2539590", "fallthroughMethodNoDefault");

        // assertNoBugInMethod("SF_SWITCH_NO_DEFAULT", "Bug2539590", "fallthroughMethod"); // desire, false positive
        // assertBugInMethod("SF_SWITCH_FALLTHROUGH", "Bug2539590", "fallthroughMethod"); // desire, false negative

        assertBugInMethod("SF_SWITCH_FALLTHROUGH", "Bug2539590", "fallthroughMethod2");

        assertNoBugInMethod("SF_SWITCH_FALLTHROUGH", "Bug2539590", "fallthroughMethod3");

        assertBugInMethod("SF_DEAD_STORE_DUE_TO_SWITCH_FALLTHROUGH", "Bug2539590", "fallthroughMethodNoDefaultClobber");
        assertBugInMethod("SF_SWITCH_NO_DEFAULT", "Bug2539590", "fallthroughMethodNoDefaultClobber");

        assertBugInMethod("SF_DEAD_STORE_DUE_TO_SWITCH_FALLTHROUGH", "Bug2539590", "fallthroughMethodClobber");
        // SF_SWITCH_NO_DEFAULT fp in fallthroughMethodClobber, without any break/continue in the switch-case
        // the presence of the default case doesn't make a difference in the bytecode

        assertBugInMethod("SF_DEAD_STORE_DUE_TO_SWITCH_FALLTHROUGH_TO_THROW", "Bug2539590", "fallthroughMethodToss");
        assertNoBugInMethod("SF_SWITCH_NO_DEFAULT", "Bug2539590", "fallthroughMethodToss");

        assertBugTypeCountBetween("SF_SWITCH_FALLTHROUGH", 1, 2);
        assertBugTypeCountBetween("SF_SWITCH_NO_DEFAULT", 3, 5);
        assertBugTypeCount("SF_DEAD_STORE_DUE_TO_SWITCH_FALLTHROUGH", 2);
        assertBugTypeCount("SF_DEAD_STORE_DUE_TO_SWITCH_FALLTHROUGH_TO_THROW", 1);
    }

    @Test
    void testBug1172() {
        performAnalysis("sfBugsNew/Bug1172.class");

        assertBugInMethod("SF_SWITCH_FALLTHROUGH", "Bug1172", "testFallThrough");
        // assertNoBugInMethod("SF_SWITCH_FALLTHROUGH", "Bug1172", "testFallThroughGood"); // desire, false negative

        assertBugTypeCountBetween("SF_SWITCH_FALLTHROUGH", 1, 2);
        assertNoBugType("SF_SWITCH_NO_DEFAULT");
        assertNoBugType("SF_DEAD_STORE_DUE_TO_SWITCH_FALLTHROUGH");
        assertNoBugType("SF_DEAD_STORE_DUE_TO_SWITCH_FALLTHROUGH_TO_THROW");
    }

    @Test
    void testBug1249() {
        performAnalysis("sfBugsNew/Bug1249.class");

        assertBugInMethod("SF_SWITCH_FALLTHROUGH", "Bug1249", "foo1");
        // assertNoBugInMethod("SF_SWITCH_FALLTHROUGH", "Bug1249", "foo2"); // desire, false negative
        // assertNoBugInMethod("SF_SWITCH_FALLTHROUGH", "Bug1249", "foo3"); // desire, false negative

        assertBugTypeCountBetween("SF_SWITCH_FALLTHROUGH", 1, 3);
        assertNoBugType("SF_SWITCH_NO_DEFAULT");
        assertNoBugType("SF_DEAD_STORE_DUE_TO_SWITCH_FALLTHROUGH");
        assertNoBugType("SF_DEAD_STORE_DUE_TO_SWITCH_FALLTHROUGH_TO_THROW");
    }
}
