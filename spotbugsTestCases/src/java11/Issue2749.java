package ghIssues;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

public class Issue2749 {
    /**
     * A VarHandle is used to perform lazy instantiation.
     */
    public static class WithVarHandle {
        public final class Value {
            // nothing else
        }

        private static final VarHandle VH;

        static {
            try {
                VH = MethodHandles.lookup().findVarHandle(WithVarHandle.class, "value", Value.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        @SuppressWarnings("unused")
        private Value value;

        public Value value() {
            var read = (Value) VH.getAcquire(this);
            return read != null ? read : computeValue();
        }

        private Value computeValue() {
            var computed = new Value();
            var witness = (Value) VH.compareAndExchangeRelease(this, null, computed);
            return witness != null ? witness : computed;
        }
    }

    /**
     * Two separate MethodHandles are used to manipulate a field.
     */
    public static class WithMethodHandles {
        private static final MethodHandle GETTER;
        private static final MethodHandle SETTER;

        static {
            var lookup = MethodHandles.lookup();
            try {
                GETTER = lookup.findGetter(WithMethodHandles.class, "field", int.class);
                SETTER = lookup.findSetter(WithMethodHandles.class, "field", int.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        @SuppressWarnings("unused")
        private int field;

        public int getField() {
            try {
                return (int) GETTER.invokeExact(this);
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }

        public void setField(int newField) {
            try {
                SETTER.invokeExact(this, newField);
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
