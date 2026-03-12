package ghIssues.issue2749;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * Two separate MethodHandles are created, but only getter is used. The field is never written
 */
public class WithMethodHandlesObjectSetterNotInvoked {
    public final class Value {
        // nothing else
    }

    private static final MethodHandle GETTER;
    private static final MethodHandle SETTER;

    static {
        var lookup = MethodHandles.lookup();
        try {
            GETTER = lookup.findGetter(WithMethodHandlesObjectSetterNotInvoked.class, "reflectiveField", Value.class);
            SETTER = lookup.findSetter(WithMethodHandlesObjectSetterNotInvoked.class, "reflectiveField", Value.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Value reflectiveField;

    public Value getReflectiveField() {
        try {
            Value reflectiveFieldValue = (Value) GETTER.invokeExact(this);
            return reflectiveFieldValue;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}