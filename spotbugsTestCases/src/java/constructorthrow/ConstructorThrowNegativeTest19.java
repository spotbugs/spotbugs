package constructorthrow;

import java.util.function.Supplier;

/**
 * The lambda which throws the exception is created and assigned to a field, but not used.
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/2695">GitHub issue #2695</a>
 */
public class ConstructorThrowNegativeTest19 {
    Supplier<String> s;

    public ConstructorThrowNegativeTest19() {
        s = ConstructorThrowNegativeTest19::supplier;
    }

    private static String supplier() {
        throw new IllegalStateException();
    }
}
