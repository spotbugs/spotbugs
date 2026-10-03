package ghIssues.issue2749;

import java.io.Serializable;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * A MethodHandle is used only to write a field of a serializable class. The field is read by serialization, so it is
 * not reported as unread.
 */
public class WithMethodHandleSetterInSerializable implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final MethodHandle SETTER;

    static {
        try {
            SETTER = MethodHandles.lookup().findSetter(WithMethodHandleSetterInSerializable.class, "reflectiveField", String.class);
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
