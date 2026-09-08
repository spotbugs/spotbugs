package ghIssues;

public class Issue3886 {

    // Case 1: Standard style - should trigger IM_BAD_CHECK_FOR_ODD
    public void standardCheck(int i) {
        if (i % 2 == 1) {
            System.out.println("Odd");
        }
    }

    // Case 2: Yoda style - should trigger IM_BAD_CHECK_FOR_ODD (was false negative)
    public void yodaCheck(int i) {
        if (1 == i % 2) {
            System.out.println("Odd");
        }
    }

    // Case 3: Yoda style with non-negative guard - should NOT trigger
    public void yodaCheckNonNeg(int i) {
        if (i >= 0 && 1 == i % 2) {
            System.out.println("Odd");
        }
    }

    // Case 4: Yoda style with Math.abs - should NOT trigger
    public void yodaCheckMathAbs(int i) {
        if (1 == Math.abs(i) % 2) {
            System.out.println("Odd");
        }
    }
}
