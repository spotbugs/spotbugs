package ghIssues;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

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
     * A VarHandle is used, but only to get the value. The field was never set.
     */
    public static class WithVarHandleNoWriting {
        public final class Value {
            // nothing else
        }

        private static final VarHandle VH;

        static {
            try {
                VH = MethodHandles.lookup().findVarHandle(WithVarHandleNoWriting.class, "value", Value.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        private Value value;

        public Value value() {
            var read = (Value) VH.getAcquire(this);
            return read;
        }
    }

    /**
     * A VarHandle is used, but only to set the value. The field was never read.
     */
    public static class WithVarHandleNoReading {
        public final class Value {
            // nothing else
        }

        private static final VarHandle VH;

        static {
            try {
                VH = MethodHandles.lookup().findVarHandle(WithVarHandleNoReading.class, "value", Value.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        private Value value;

        private void computeValue() {
            var computed = new Value();
            VH.setRelease(this, computed);
        }
    }

    /**
     * A VarHandle is invoked only once - both to get and set the value
     */
    public static class WithVarHandleReadingAndWritingInOneInvocation {
        public final class Value {
            // nothing else
        }

        private static final VarHandle VH;

        static {
            try {
                VH = MethodHandles.lookup().findVarHandle(WithVarHandleReadingAndWritingInOneInvocation.class, "value", Value.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        private Value value;

        private Value computeValue() {
            var computed = new Value();
            var witness = (Value) VH.compareAndExchangeRelease(this, null, computed);
            return witness != null ? witness : computed;
        }
    }

    /**
     * A VarHandle is created, but never invoked
     */
    public static class WithVarHandleNeverInvoked {
        public final class Value {
            // nothing else
        }

        private static final VarHandle VH;

        static {
            try {
                VH = MethodHandles.lookup().findVarHandle(WithVarHandleNeverInvoked.class, "value", Value.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        private Value value;
    }


    /**
     * Two separate MethodHandles are used to manipulate a primitive field.
     */
    public static class WithMethodHandlesPrimitive {
        private static final MethodHandle GETTER;
        private static final MethodHandle SETTER;

        static {
            var lookup = MethodHandles.lookup();
            try {
                GETTER = lookup.findGetter(WithMethodHandlesPrimitive.class, "field", int.class);
                SETTER = lookup.findSetter(WithMethodHandlesPrimitive.class, "field", int.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

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

    /**
     * Two separate MethodHandles are used to manipulate an object field.
     */
    public static class WithMethodHandlesObject {
        public final class Value {
            // nothing else
        }

        private static final MethodHandle GETTER;
        private static final MethodHandle SETTER;

        static {
            var lookup = MethodHandles.lookup();
            try {
                GETTER = lookup.findGetter(WithMethodHandlesObject.class, "field", Value.class);
                SETTER = lookup.findSetter(WithMethodHandlesObject.class, "field", Value.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        private Value field;

        public Value getField() {
            try {
                return (Value) GETTER.invokeExact(this);
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }

        public void setField(Value newField) {
            try {
                SETTER.invokeExact(this, newField);
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Two separate MethodHandles are created, but only getter is used. The field is never written
     */
    public static class WithMethodHandlesObjectSetterNotInvoked {
        public final class DummyValue {
            // nothing else
        }

        private static final MethodHandle GETTER;
        private static final MethodHandle SETTER;

        static {
            var lookup = MethodHandles.lookup();
            try {
                GETTER = lookup.findGetter(WithMethodHandlesObjectSetterNotInvoked.class, "reflectiveField", DummyValue.class);
                SETTER = lookup.findSetter(WithMethodHandlesObjectSetterNotInvoked.class, "reflectiveField", DummyValue.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        private DummyValue reflectiveField;

        public DummyValue getReflectiveField() {
            try {
                DummyValue reflectiveFieldValue = (DummyValue) GETTER.invokeExact(this);
                return reflectiveFieldValue;
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Two separate MethodHandles are created, but only setter is used. The field is never read
     */
    public static class WithMethodHandlesObjectGetterNotInvoked {
        public final class DummyValue {
            // nothing else
        }

        private static final MethodHandle GETTER;
        private static final MethodHandle SETTER;

        static {
            var lookup = MethodHandles.lookup();
            try {
                GETTER = lookup.findGetter(WithMethodHandlesObjectGetterNotInvoked.class, "reflectiveField", DummyValue.class);
                SETTER = lookup.findSetter(WithMethodHandlesObjectGetterNotInvoked.class, "reflectiveField", DummyValue.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        private DummyValue reflectiveField;

        public void setReflectiveField(DummyValue newField) {
            try {
                SETTER.invokeExact(this, newField);
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Two separate MethodHandles are created, but neither is invoked
     */
    public static class WithMethodHandlesNeverInvoked {
        public final class DummyValue {
            // nothing else
        }

        private static final MethodHandle GETTER;
        private static final MethodHandle SETTER;

        static {
            var lookup = MethodHandles.lookup();
            try {
                GETTER = lookup.findGetter(WithMethodHandlesNeverInvoked.class, "reflectiveField", DummyValue.class);
                SETTER = lookup.findSetter(WithMethodHandlesNeverInvoked.class, "reflectiveField", DummyValue.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new ExceptionInInitializerError(e);
            }
        }

        private DummyValue reflectiveField;
    }

    /**
     * Create AtomicFieldUpdater, but invoke only getter.
     */
    public static class WithAtomicUpdaterSetterNotInvoked {
        public final class DummyValue {
            // nothing else
        }

        private static final AtomicReferenceFieldUpdater<WithAtomicUpdaterSetterNotInvoked,
            DummyValue> UPDATER = AtomicReferenceFieldUpdater.newUpdater(
            WithAtomicUpdaterSetterNotInvoked.class, DummyValue.class, "reflectiveField");

        private volatile DummyValue reflectiveField;

        public DummyValue getReflectiveField() {
            try {
                DummyValue reflectiveFieldValue = (DummyValue) UPDATER.get(this);
                return reflectiveFieldValue;
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Create AtomicFieldUpdater, but invoke only setter.
     */
    public static class WithAtomicUpdaterGetterNotInvoked {
        public final class DummyValue {
            // nothing else
        }

        private static final AtomicReferenceFieldUpdater<WithAtomicUpdaterGetterNotInvoked,
            DummyValue> UPDATER = AtomicReferenceFieldUpdater.newUpdater(
            WithAtomicUpdaterGetterNotInvoked.class, DummyValue.class, "reflectiveField");

        private volatile DummyValue reflectiveField;

        public void setReflectiveField(DummyValue newField) {
            try {
                UPDATER.lazySet(this, newField);
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }
    }

    /**
     * Create AtomicFieldUpdater and read/write in one invocation
     */
    public static class WithAtomicUpdaterReadingAndWritingInOneInvocation {
        public final class DummyValue {
            // nothing else
        }

        private static final AtomicReferenceFieldUpdater<WithAtomicUpdaterReadingAndWritingInOneInvocation,
            DummyValue> UPDATER = AtomicReferenceFieldUpdater.newUpdater(
            WithAtomicUpdaterReadingAndWritingInOneInvocation.class, DummyValue.class, "reflectiveField");

        private volatile DummyValue reflectiveField;

        public void setReflectiveField(DummyValue newField) {
            try {
                UPDATER.compareAndSet(this, null, newField);
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public static class WithAtomicUpdaters {
        public final class DummyValue {
            // nothing else
        }

        private static final AtomicReferenceFieldUpdater<WithAtomicUpdaters, DummyValue> REREFENCE_UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(WithAtomicUpdaters.class, DummyValue.class, "reflectiveObjectField");

        private static final AtomicIntegerFieldUpdater<WithAtomicUpdaters> INT_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(WithAtomicUpdaters.class, "reflectiveIntField");

        private static final AtomicLongFieldUpdater<WithAtomicUpdaters> LONG_UPDATER =
            AtomicLongFieldUpdater.newUpdater(WithAtomicUpdaters.class, "reflectiveLongField");


        private volatile DummyValue reflectiveObjectField;
        private volatile int reflectiveIntField;
        private volatile long reflectiveLongField;

        public void setReflectiveFields(DummyValue newObject, int newInt, long newLong) {
            try {
                REREFENCE_UPDATER.weakCompareAndSet(this, null, newObject);
                LONG_UPDATER.getAndSet(this, newLong);
                INT_UPDATER.getAndIncrement(this);
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
