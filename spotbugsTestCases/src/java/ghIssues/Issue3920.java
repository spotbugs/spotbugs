package ghIssues;

/**
 * Test case for GitHub issue #3920:
 * RCN false negative when non-null value is on the left side of null comparison.
 *
 * Both m1() and m2() should be reported with RCN since System.out is a known
 * non-null field and both comparisons are semantically equivalent.
 */
public class Issue3920 {

    /**
     * Operand order: non-null == null (compiles to GETSTATIC + IFNONNULL).
     * Should report RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE.
     */
    public static void m1() {
        if (System.out == null) {
            throw new RuntimeException("System.out is null");
        }
    }

    /**
     * Operand order: null == non-null (compiles to ACONST_NULL + GETSTATIC + IF_ACMPNE).
     * Should report RCN_REDUNDANT_COMPARISON_OF_NULL_AND_NONNULL_VALUE.
     */
    public static void m2() {
        if (null == System.out) {
            throw new RuntimeException("System.out is null");
        }
    }
}
