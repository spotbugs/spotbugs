package ghIssues;

import edu.umd.cs.findbugs.annotations.ExpectWarning;

/**
 * <a href="https://github.com/spotbugs/spotbugs/issues/3469">#3469</a>.
 */
class Issue3469 {
    @ExpectWarning("IL_INFINITE_RECURSIVE_LOOP")
    public static void infiniteLoopMethod() {
        boolean shouldLoop = getCondition();
        if (shouldLoop) {
            infiniteLoopMethod();
        } else {
            System.out.println("This branch should never execute.");
        }
    }

    public static boolean getCondition() {
        return true;
    }
}
