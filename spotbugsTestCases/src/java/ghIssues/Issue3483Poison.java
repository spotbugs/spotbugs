package ghIssues;

/**
 * Companion of {@link Issue3483}: leaks the {@code inAssert} state across a class boundary.
 * See https://github.com/spotbugs/spotbugs/issues/3483
 */
public class Issue3483Poison {

    public static boolean $assertionsDisabled;

    public static void poison() {
        if ($assertionsDisabled) {
            return;
        }
    }
}
