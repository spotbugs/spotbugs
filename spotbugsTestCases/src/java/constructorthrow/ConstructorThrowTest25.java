package constructorthrow;

import java.util.Objects;

/**
 * Based on <a href="https://github.com/spotbugs/spotbugs/issues/3821">GitHub issue #3821</a>
 *
 */
public class ConstructorThrowTest25 {
    public ConstructorThrowTest25(Object param) {
        Objects.requireNonNull(param);
    }
}
