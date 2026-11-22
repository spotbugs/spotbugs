package constructorthrow;

import java.util.Objects;
import java.util.function.Supplier;

public class Issue4439 {
    public static class ImplicitConstant {
        private final int limit = 10;

        public ImplicitConstant() {
            consume(limit);
        }

        private void consume(int value) {
        }
    }

    public static class ExplicitConstant {
        private final int limit = 10;

        public ExplicitConstant() {
            consume(this.limit);
        }

        private void consume(int value) {
        }
    }

    public static class StaticConstant {
        private static final int LIMIT = 10;

        public StaticConstant() {
            consume(this.LIMIT);
        }

        private void consume(int value) {
        }
    }

    public static class ExplicitThis {
        public ExplicitThis() {
            Objects.requireNonNull(this);
        }
    }

    public static class ThisWithMessage {
        public ThisWithMessage() {
            Objects.requireNonNull(this, "unused");
        }
    }

    public static class ThisWithSupplier {
        public ThisWithSupplier() {
            Objects.requireNonNull(this, (Supplier<String>) null);
        }
    }

    public static class InstanceHelper {
        public InstanceHelper() {
            checkThis();
        }

        private void checkThis() {
            Objects.requireNonNull(this);
        }
    }

    public static class NullableParameter {
        public NullableParameter(Object value) {
            Objects.requireNonNull(value);
        }
    }

    public static class NullableField {
        private final Object value;

        public NullableField(Object value) {
            this.value = value;
            Objects.requireNonNull(this.value);
        }
    }

    public static class StaticHelper {
        public StaticHelper(Object value) {
            check(value);
        }

        private static void check(Object value) {
            Objects.requireNonNull(value);
        }
    }

    public static class ThisThenThrow {
        public ThisThenThrow() {
            Objects.requireNonNull(this);
            throw new IllegalStateException();
        }
    }

    public static class NullableWithThisSupplier implements Supplier<String> {
        public NullableWithThisSupplier(Object value) {
            Objects.requireNonNull(value, this);
        }

        @Override
        public String get() {
            return "value";
        }
    }

    public static class OtherInstanceConstant {
        private final int limit = 10;

        public OtherInstanceConstant(OtherInstanceConstant other) {
            consume(other.limit);
        }

        private void consume(int value) {
        }
    }

    public static class AlwaysNull {
        public AlwaysNull() {
            Objects.requireNonNull((Object) null);
        }
    }

    public static class ThrowingMessage {
        public ThrowingMessage() {
            Objects.requireNonNull(this, message());
        }

        private String message() {
            throw new IllegalStateException();
        }
    }
}
