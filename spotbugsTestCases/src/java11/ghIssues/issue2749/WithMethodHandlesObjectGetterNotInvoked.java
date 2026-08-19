package ghIssues.issue2749;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * Two separate MethodHandles are created, but only setter is used. The field is never read
 */
public class WithMethodHandlesObjectGetterNotInvoked {
    public final class Value {
        // nothing else
    }

    private static final MethodHandle GETTER;
    private static final MethodHandle SETTER;

    static {
        var lookup = MethodHandles.lookup();
        try {
            GETTER = lookup.findGetter(WithMethodHandlesObjectGetterNotInvoked.class, "reflectiveField", Value.class);
            SETTER = lookup.findSetter(WithMethodHandlesObjectGetterNotInvoked.class, "reflectiveField", Value.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Value reflectiveField;

    public void setReflectiveField(Value newField) {
        try {
            SETTER.invokeExact(this, newField);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}