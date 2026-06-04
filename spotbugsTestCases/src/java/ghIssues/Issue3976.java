package ghIssues;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/3976">#3976</a>.
 */
public class Issue3976 {
    static boolean hasSideEffect() {
        System.out.println("Side effect executed!");
        return true;
    }

    @ExpectWarning("NS_NON_SHORT_CIRCUIT")
    public void testTruePositive(boolean input) {
        if (input | hasSideEffect()) {
            System.out.println("Path A");
        }
    }

    @ExpectWarning("NS_NON_SHORT_CIRCUIT")
    public void testFalseNegative(boolean input) {
        if (true | hasSideEffect()) {
            System.out.println("Path B");
        }
    }
}
