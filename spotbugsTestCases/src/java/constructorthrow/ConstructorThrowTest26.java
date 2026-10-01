package constructorthrow;

import java.util.Objects;

/**
 * Based on <a href="https://github.com/spotbugs/spotbugs/issues/4259">GitHub issue #4259</a>
 * Non-sealed abstract class can have malicious subclass.
 */
public abstract class ConstructorThrowTest26 {

    public static final class Child extends ConstructorThrowTest26 {

        public Child(final Object o) {
            super(o);
        }
    }

    private final Object o;

    public ConstructorThrowTest26(final Object o) {
        this.o = Objects.requireNonNull(o, "o");
    }

    Object getO() {
        return o;
    }
}
