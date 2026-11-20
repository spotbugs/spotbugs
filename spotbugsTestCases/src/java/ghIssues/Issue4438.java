package ghIssues;

/**
 * Test cases for GitHub issue #4438:
 * IL_INFINITE_LOOP is not reported when the operands of an int comparison are reversed.
 *
 * <p>
 * {@code s.trim().length() == 0} is compiled by javac to a single-operand {@code ifne}, because comparing an int against
 * the literal 0 needs no second operand. Swapping the operands to {@code 0 == s.trim().length()} materialises the 0 with
 * {@code iconst_0} and compares the two values with {@code if_icmpne}. The two spellings express exactly the same
 * condition, so the detector must treat them the same.
 * </p>
 */
public class Issue4438 {

    // Operands are not reversed: javac emits `ifne`, one operand. Reported today.
    public void infiniteLoopPlainOperandOrder() {
        String s = "foo";
        while (s.trim().length() == 0) {
        }
    }

    // Same condition with the operands reversed: javac emits `iconst_0` + `if_icmpne`, two operands.
    public void infiniteLoopReversedOperandOrder() {
        String s = "foo";
        while (0 == s.trim().length()) {
        }
    }
}
