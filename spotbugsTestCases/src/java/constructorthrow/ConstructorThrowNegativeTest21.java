package constructorthrow;

import java.util.ArrayList;
import java.util.Collection;

/**
 * The Exception is thrown before the java.lang.Object constructor exits, so this is compliant, since in this case
 * the JVM will not execute an object's finalizer.
 * @see <a href="https://github.com/spotbugs/spotbugs/issues/3822">GitHub issue</a>
 */
public class ConstructorThrowNegativeTest21 {
    public ConstructorThrowNegativeTest21(Collection<?> foo) {
        this(new ArrayList<>(foo), bar());
    }

    private ConstructorThrowNegativeTest21(Collection<?> foo, Object bar) {
    }

    private static Object bar() throws RuntimeException {
        return null;
    }
}
