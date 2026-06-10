package edu.umd.cs.findbugs.detect;

import org.junit.jupiter.api.Test;

import edu.umd.cs.findbugs.AbstractIntegrationTest;

/**
 * Several bytecode-pattern detectors only recognize the javac-optimized
 * single-operand null guard ({@code IFNULL}/{@code IFNONNULL}) and miss the
 * equivalent Yoda-style guard ({@code null == field}), which javac compiles to
 * {@code IF_ACMPEQ}/{@code IF_ACMPNE}. These tests pin the expected behavior:
 * the Yoda form must be reported just like {@code field == null}.
 *
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/4138">#4138</a>
 */
class YodaNullCheckFalseNegativeTest extends AbstractIntegrationTest {

    @Test
    void doubleCheck() {
        performAnalysis("ghIssues/issue4138/DoubleCheckYoda.class");
        assertBugInMethod("DC_DOUBLECHECK", "DoubleCheckYoda", "getNormal");
        assertBugInMethod("DC_DOUBLECHECK", "DoubleCheckYoda", "getYoda");
    }

    @Test
    void syncAndNullCheckField() {
        performAnalysis("ghIssues/issue4138/SyncNullCheckYoda.class");
        assertBugInMethod("NP_SYNC_AND_NULL_CHECK_FIELD", "SyncNullCheckYoda", "useNormal");
        assertBugInMethod("NP_SYNC_AND_NULL_CHECK_FIELD", "SyncNullCheckYoda", "useYoda");
    }

    @Test
    void spinLoop() {
        performAnalysis("ghIssues/issue4138/SpinLoopYoda.class");
        assertBugInMethod("SP_SPIN_ON_FIELD", "SpinLoopYoda", "spinNormal");
        assertBugInMethod("SP_SPIN_ON_FIELD", "SpinLoopYoda", "spinYoda");
    }
}
