package edu.umd.cs.findbugs.detect;

import edu.umd.cs.findbugs.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;

/**
 * Regression test for issue #4147: {@code NP_NULL_ON_SOME_PATH} verdict must
 * not depend on the operand order of a reference null check. Both
 * {@code x == null} (direct form, single-operand {@code IFNULL}/{@code IFNONNULL})
 * and {@code null == x} (Yoda form, two-operand {@code IF_ACMPEQ}/{@code IF_ACMPNE})
 * are identical conditions, so the null-dataflow analysis must reach the same
 * verdict on the non-null (else) branch that dereferences the field.
 */
class Issue4147Test extends AbstractIntegrationTest {

    @Test
    void testDirectForm() {
        performAnalysis("ghIssues/Issue4147.class");
        assertNoBugInMethod("NP_NULL_ON_SOME_PATH", "ghIssues.Issue4147", "directForm");
    }

    @Test
    void testYodaForm() {
        performAnalysis("ghIssues/Issue4147.class");
        assertNoBugInMethod("NP_NULL_ON_SOME_PATH", "ghIssues.Issue4147", "yodaForm");
    }
}
