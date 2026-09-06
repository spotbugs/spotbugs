package ghIssues.issue2749;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * Two separate MethodHandles manipulate a primitive field, but both are kept in fields declared as
 * Object, so the declared type of the accessor fields is wider than the type of the stored handles.
 */
public class WithMethodHandlesInObjectField {
    private static final Object GETTER;
    private static final Object SETTER;

    static {
        var lookup = MethodHandles.lookup();
        try {
            GETTER = lookup.findGetter(WithMethodHandlesInObjectField.class, "field", int.class);
            SETTER = lookup.findSetter(WithMethodHandlesInObjectField.class, "field", int.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private int field;

    public int getField() {
        try {
            return (int) ((MethodHandle) GETTER).invokeExact(this);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }

    public void setField(int newField) {
        try {
            ((MethodHandle) SETTER).invokeExact(this, newField);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}
