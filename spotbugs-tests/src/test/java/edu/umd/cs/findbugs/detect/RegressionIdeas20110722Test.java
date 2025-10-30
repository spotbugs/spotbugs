package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

class RegressionIdeas20110722Test extends AbstractIntegrationTest {

    // Problems in null checks, see https://github.com/spotbugs/spotbugs/issues/2890
    @Disabled
    @Test
    void testArgumentAssertions() {
        performAnalysis("bugIdeas/Ideas_2011_07_22.class");

        assertNoBugType("NP_NULL_ON_SOME_PATH");
        assertBugTypeCount("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", 4);
        assertBugTypeCount("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", 2);

        assertNoBugInMethod("NP_NULL_ON_SOME_PATH", "Ideas_2011_07_22", "getHashCode");
        assertNoBugInMethod("NP_NULL_ON_SOME_PATH", "Ideas_2011_07_22", "getHashCode1");
        assertNoBugInMethod("NP_NULL_ON_SOME_PATH", "Ideas_2011_07_22", "getHashCode2");
        assertNoBugInMethod("NP_NULL_ON_SOME_PATH", "Ideas_2011_07_22", "getHashCode3");
        assertRCNBug("getHashCode3", "x", 34);
        assertNoBugInMethod("NP_NULL_ON_SOME_PATH", "Ideas_2011_07_22", "getHashCode4");
        assertRCNBug("getHashCode4", "x", 41);
        assertNoBugInMethod("NP_NULL_ON_SOME_PATH", "Ideas_2011_07_22", "getHashCode5");
        assertRCNBug("getHashCode5", "x", 48);
        assertRCNBug("getHashCode6", "x", 55);
        assertBugInMethodAtVariable("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", "Ideas_2011_07_22", "getHashCode6", "x");
        assertBugInMethodAtVariable("NP_PARAMETER_MUST_BE_NONNULL_BUT_MARKED_AS_NULLABLE", "Ideas_2011_07_22", "getHashCode7", "x");
    }

    private void assertRCNBug(String method, String variable, int line) {
        assertBugAtVar("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE", "Ideas_2011_07_22", method, variable, line);
    }
}
