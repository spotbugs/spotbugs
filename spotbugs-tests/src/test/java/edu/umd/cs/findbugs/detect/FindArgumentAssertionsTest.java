package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import edu.umd.cs.findbugs.annotations.Confidence;

class FindArgumentAssertionsTest extends AbstractIntegrationTest {

    private static final String BUG_TYPE = "AA_ASSERTION_OF_ARGUMENTS";

    @Test
    void testArgumentAssertions() {
        performAnalysis("ArgumentAssertions.class");

        assertBugTypeCount(BUG_TYPE, 23);

        assertDABug("getAbsAdd", 4);
        assertDABug("getAbsAdd", 5);
        assertDABug("getAbsAdd2", 14);
        assertDABug("getAbsAdd2", 15);
        assertDABug("getAbsAdd3", 28);
        assertDABug("getAbsAdd3", 29);
        assertDABug("getAbsAdd3", 30);
        assertDABug("getAbsAdd4", 39);
        assertDABug("print", 47);
        assertDABug("indirect", 59);
        assertDABug("compBool1", 64);
        assertDABug("compBool2", 70);
        assertDABug("getAbs", 76);
        assertDABug("getAbs", 83);
        assertDABug("compShor", 90);
        assertDABug("getAbs", 96);
        assertDABug("compLong", 103);
        assertDABug("getAbs", 109);
        assertDABug("compFloat", 116);
        assertDABug("getAbs", 122);
        assertDABug("compDouble", 129);
        assertDABug("indirect", 136);
        // assertDABug("indirect2", 143); -- indirect assertions of arguments are not supported yet (except copies)
        assertNoBugInMethod(BUG_TYPE, "ArgumentAssertions", "literal");
        assertNoBugInMethod(BUG_TYPE, "ArgumentAssertions", "literalAndMessage");
        assertNoBugInMethod(BUG_TYPE, "ArgumentAssertions", "literalAndMessageStr");
        assertNoBugInMethod(BUG_TYPE, "ArgumentAssertions", "conditionallyInMessage");
        assertNoBugInMethod(BUG_TYPE, "ArgumentAssertions", "privateMethod");
        assertNoBugInMethod(BUG_TYPE, "ArgumentAssertions", "privateFinalMethod");
        assertNoBugInMethod(BUG_TYPE, "ArgumentAssertions", "privateStaticMethod");
        assertDABug("assertingArgInFor", 198);
        // assertDABug("lambda$assertingArgInStream$0", 206); // assertions inside streams are not supported yet
    }

    private void assertDABug(String method, int line) {
        assertBugInMethodAtLineWithConfidence(BUG_TYPE, "ArgumentAssertions", method, line, Confidence.LOW);
    }
}
