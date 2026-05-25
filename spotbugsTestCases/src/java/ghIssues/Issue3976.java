package ghIssues;

import edu.umd.cs.findbugs.annotations.ExpectWarning;
import edu.umd.cs.findbugs.annotations.NoWarning;

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
    public void testFalseNegative() {
        if (true | hasSideEffect()) {
            System.out.println("Path B");
        }
    }

    @ExpectWarning("NS_NON_SHORT_CIRCUIT")
    public void testConstantFalseAnd() {
        if (false & hasSideEffect()) {
            System.out.println("Path C");
        }
    }

    @NoWarning("NS_NON_SHORT_CIRCUIT,NS_DANGEROUS_NON_SHORT_CIRCUIT")
    public boolean bothConstants() {
        return true | false;
    }
}
