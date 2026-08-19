package ghIssues.issue2749;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * Two separate MethodHandles are used to manipulate an object field.
 */
public class WithMethodHandlesObject {
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