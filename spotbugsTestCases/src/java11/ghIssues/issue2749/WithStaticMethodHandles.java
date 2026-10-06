package ghIssues.issue2749;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * Two separate MethodHandles are used to manipulate a static object field.
 */
public class WithStaticMethodHandles {
    public static final class Value {
        // nothing else
    }

    private static final MethodHandle GETTER;
    private static final MethodHandle SETTER;

    static {
        var lookup = MethodHandles.lookup();
        try {
            GETTER = lookup.findStaticGetter(WithStaticMethodHandles.class, "field", Value.class);
            SETTER = lookup.findStaticSetter(WithStaticMethodHandles.class, "field", Value.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static Value field;

    public static Value getField() {
        try {
            return (Value) GETTER.invokeExact();
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    public static void setField(Value newField) {
        try {
            SETTER.invokeExact(newField);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}
