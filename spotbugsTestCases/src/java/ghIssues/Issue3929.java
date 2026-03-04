package ghIssues;

/**
 * Test cases for GitHub issue #3929:
 * SA_LOCAL_SELF_ASSIGNMENT incorrectly reported in nested try-switch structure.
 */
public class Issue3929 {

    // False positive (now fixed): ++a inside switch inside try-catch should NOT report SA
    public void reproduceFalsePositive() {
        try {
            int a = 0;
            switch (a) {
                default:
                    ++a;
            }
        } catch (Exception b) {
        }
    }

    // Variant: pre-increment before catch, still should NOT report SA
    public void incrementBeforeCatch() {
        try {
            int x = 1;
            switch (x) {
                case 1:
                    ++x;
                    break;
                default:
                    break;
            }
        } catch (RuntimeException e) {
        }
    }

    // True positive: real self-assignment should still be reported
    @SuppressWarnings("SA_LOCAL_SELF_ASSIGNMENT")
    public void realSelfAssignment() {
        int a = 1;
        a = a; // real SA_LOCAL_SELF_ASSIGNMENT
    }
}
