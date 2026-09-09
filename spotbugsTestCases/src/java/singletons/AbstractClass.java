package singletons;

import java.io.Serializable;
import java.util.Objects;
/**
 * See <a href="https://github.com/spotbugs/spotbugs/issues/2934">#2934</a>
 */
public abstract class AbstractClass implements Serializable {
    public static class Nested extends AbstractClass {
        private static final long serialVersionUID = 1L;

        private final String appTag;

        Nested(final String appTag) {
            this.appTag = appTag;
        }

        @Override
        String abstractMethod() {
            return appTag;
        }
    }

    private static final AbstractClass NESTED = new Nested(null);
    private static final long serialVersionUID = 1L;

    public static AbstractClass nested() {
        return NESTED;
    }

    abstract String abstractMethod();

    @Override
    public final int hashCode() {
        return Objects.hash(abstractMethod());
    }

    @Override
    public final boolean equals(final Object obj) {
        if (this == obj || obj instanceof AbstractClass) {
            AbstractClass other = (AbstractClass) obj;
            return Objects.equals(abstractMethod(), other.abstractMethod());
        }
        return false;
    }
}
