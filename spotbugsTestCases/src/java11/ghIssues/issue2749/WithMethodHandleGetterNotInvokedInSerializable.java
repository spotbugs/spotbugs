package ghIssues.issue2749;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * Two separate MethodHandles are created for a field of a serializable class, but only the setter is used. The field is
 * read by serialization, so neither the field nor the unused getter is reported.
 */
public class WithMethodHandleGetterNotInvokedInSerializable implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final MethodHandle GETTER;
    private static final MethodHandle SETTER;

    static {
        var lookup = MethodHandles.lookup();
        try {
            GETTER = lookup.findGetter(WithMethodHandleGetterNotInvokedInSerializable.class, "reflectiveField", String.class);
            SETTER = lookup.findSetter(WithMethodHandleGetterNotInvokedInSerializable.class, "reflectiveField", String.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private String reflectiveField;

    public void setReflectiveField(String newField) {
        try {
            SETTER.invokeExact(this, newField);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}
