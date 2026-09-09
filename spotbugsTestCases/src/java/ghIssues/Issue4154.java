package ghIssues;

/**
 * Ternary-form lazy singleton initialization.
 *
 * <p>Semantically equivalent to {@link Issue3280}, but the null guard and assignment
 * are written as a ternary instead of an {@code if} statement.
 * {@code SING_SINGLETON_GETTER_NOT_SYNCHRONIZED} must still fire.
 *
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/4154">#4154</a>
 */
public class Issue4154 {
    private static volatile Issue4154 instance = null;

    public static Issue4154 getInstance() {
        instance = null == instance ? new Issue4154() : instance;
        instance.factorial(5);
        return instance;
    }

    private Issue4154() {
    }

    private int factorial(int n) {
        if (n == 0) {
            return 1;
        }
        return n * factorial(n - 1);
    }
}
