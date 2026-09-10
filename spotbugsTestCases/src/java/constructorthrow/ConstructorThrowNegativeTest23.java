package constructorthrow;

import java.util.Objects;

/**
 * Based on <a href="https://github.com/spotbugs/spotbugs/issues/4259">GitHub issue #4259</a>
 *
 */
public abstract sealed class ConstructorThrowNegativeTest23 permits ConstructorThrowNegativeTest23.FinalChild, ConstructorThrowNegativeTest23.SealedChild {

    public static final class FinalChild extends ConstructorThrowNegativeTest23 {

        public FinalChild(final Object o) {
            super(o);
        }
    }

    public static sealed class SealedChild extends ConstructorThrowNegativeTest23 permits SealedChild.FinalGrandChild {

        public SealedChild(final Object o) {
            super(o);
        }

        public static final class FinalGrandChild extends SealedChild {

            public FinalGrandChild(final Object o) {
                super(o);
            }
        }
    }

    private final Object o;

    public ConstructorThrowNegativeTest23(final Object o) {
        this.o = Objects.requireNonNull(o, "o");
    }

    Object getO() {
        return o;
    }
}