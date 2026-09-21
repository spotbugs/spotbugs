package constructorthrow;

import java.util.Objects;

/**
 * Based on <a href="https://github.com/spotbugs/spotbugs/issues/4259">GitHub issue #4259</a>
 * The abstract class is sealed, the permitted keyword is not necessary when there is only one inner subclass.
 * The subclass however can be maliciously subclassed, as it's non-sealed.
 */
public abstract sealed class ConstructorThrowTest27 {

    public static non-sealed class NonSealedChild extends ConstructorThrowTest27 {

        public NonSealedChild(final Object o) {
            super(o);
        }
    }

    private final Object o;

    public ConstructorThrowTest27(final Object o) {
        this.o = Objects.requireNonNull(o, "o");
    }

    Object getO() {
        return o;
    }
}