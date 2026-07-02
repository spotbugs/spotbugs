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
        assertBugTypeCount("DC_DOUBLECHECK", 2);
        assertBugInMethodAtField("DC_DOUBLECHECK", "DoubleCheckYoda", "getNormal", "normal");
        assertBugInMethodAtField("DC_DOUBLECHECK", "DoubleCheckYoda", "getYoda", "yoda");
    }

    @Test
    void syncAndNullCheckField() {
        performAnalysis("ghIssues/issue4138/SyncNullCheckYoda.class");
        assertBugTypeCount("NP_SYNC_AND_NULL_CHECK_FIELD", 2);
        assertBugInMethodAtField("NP_SYNC_AND_NULL_CHECK_FIELD", "SyncNullCheckYoda", "useNormal", "normal");
        assertBugInMethodAtField("NP_SYNC_AND_NULL_CHECK_FIELD", "SyncNullCheckYoda", "useYoda", "yoda");
    }

    @Test
    void spinLoop() {
        performAnalysis("ghIssues/issue4138/SpinLoopYoda.class");
        assertBugTypeCount("SP_SPIN_ON_FIELD", 2);
        assertBugInMethodAtField("SP_SPIN_ON_FIELD", "SpinLoopYoda", "spinNormal", "normal");
        assertBugInMethodAtField("SP_SPIN_ON_FIELD", "SpinLoopYoda", "spinYoda", "yoda");
    }
}
