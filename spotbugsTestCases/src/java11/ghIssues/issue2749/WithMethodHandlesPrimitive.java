package ghIssues.issue2749;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * Two separate MethodHandles are used to manipulate a primitive field.
 */
public class WithMethodHandlesPrimitive {
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