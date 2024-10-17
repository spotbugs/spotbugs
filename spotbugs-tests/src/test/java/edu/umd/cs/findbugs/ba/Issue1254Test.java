package edu.umd.cs.findbugs.ba;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnJre;
import org.junit.jupiter.api.condition.JRE;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

/**
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/1254">GitHub issue</a>
 */
class Issue1254Test extends AbstractIntegrationTest {

    // These tests require java 11 or better to run
    private static final String[] CLASS_LIST = { "../java11/module-info.class", "../java11/Issue1254.class",
        "../java11/Issue1254$Inner.class", "../java11/Issue1254$1.class", };

    /**
     * Test that accessing private members of a nested class doesn't result in unresolvable reference problems.
     */
    @Test
    void testUnresolvableReference() {
        performAnalysis(CLASS_LIST);
        assertNoBugType("VR_UNRESOLVABLE_REFERENCE");
    }

    /**
     * Test that accessing private members of a nested class doesn't result in uncalled method problems.
     */
    @Test
    @DisabledOnJre(JRE.JAVA_8)
    void testUncalledPrivateMethod() {
        performAnalysis(CLASS_LIST);
        assertBugTypeCount("UPM_UNCALLED_PRIVATE_METHOD", 2);
        assertBugInMethod("UPM_UNCALLED_PRIVATE_METHOD", "Issue1254", "uncalledOuterMethod");
        assertBugInMethod("UPM_UNCALLED_PRIVATE_METHOD", "Issue1254$Inner", "uncalledInnerMethod");
    }
}
