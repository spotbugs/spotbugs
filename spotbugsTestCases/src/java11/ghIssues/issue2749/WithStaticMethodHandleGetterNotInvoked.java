package ghIssues.issue2749;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * Two separate MethodHandles are created for a static field, but only the setter is used. The field is never read.
 */
public class WithStaticMethodHandleGetterNotInvoked {
    public static final class Value {
        // nothing else
    }

    private static final MethodHandle GETTER;
    private static final MethodHandle SETTER;

    static {
        var lookup = MethodHandles.lookup();
        try {
            GETTER = lookup.findStaticGetter(WithStaticMethodHandleGetterNotInvoked.class, "reflectiveField", Value.class);
            SETTER = lookup.findStaticSetter(WithStaticMethodHandleGetterNotInvoked.class, "reflectiveField", Value.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static Value reflectiveField;

    public static void setReflectiveField(Value newField) {
        try {
            SETTER.invokeExact(newField);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}
