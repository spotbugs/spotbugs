package ghIssues.issue2749;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * Two separate MethodHandles are created for a static field, but only getter is used. The field is never written
 */
public class WithStaticMethodHandleSetterNotInvoked {
    public static final class Value {
        // nothing else
    }

    private static final MethodHandle GETTER;
    private static final MethodHandle SETTER;

    static {
        var lookup = MethodHandles.lookup();
        try {
            GETTER = lookup.findStaticGetter(WithStaticMethodHandleSetterNotInvoked.class, "reflectiveField", Value.class);
            SETTER = lookup.findStaticSetter(WithStaticMethodHandleSetterNotInvoked.class, "reflectiveField", Value.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static Value reflectiveField;

    public static Value getReflectiveField() {
        try {
            Value reflectiveFieldValue = (Value) GETTER.invokeExact();
            return reflectiveFieldValue;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}
