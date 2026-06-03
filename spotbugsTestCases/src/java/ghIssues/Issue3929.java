package ghIssues;

/**
 * Test cases for GitHub issue #3929:
 * SA_LOCAL_SELF_ASSIGNMENT incorrectly reported in nested try-switch structure.
 */
public class Issue3929 {

    // Minimized false positive (now fixed): ++a inside try-catch should NOT report SA
    public void incrementInsideTryCatch() {
        try {
            int a = 0;
            ++a;
        } catch (Exception b) {
        }
    }

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

    // Variant: pre-decrement before catch, also should NOT report SA
    public void decrementBeforeCatch() {
        try {
            int x = 1;
            switch (x) {
                case 1:
                    --x;
                    break;
                default:
                    break;
            }
        } catch (RuntimeException e) {
        }
    }

    // True positive: real self-assignment should still be reported
    public void realSelfAssignment() {
        int a = 1;
        a = a; // real SA_LOCAL_SELF_ASSIGNMENT
    }

    // True positive: self-assignment inside try-catch should still be reported
    public void selfAssignmentInsideTryCatch() {
        try {
            int a = 1;
            a = a; // real SA_LOCAL_SELF_ASSIGNMENT
        } catch (Exception e) {
        }
    }
}
