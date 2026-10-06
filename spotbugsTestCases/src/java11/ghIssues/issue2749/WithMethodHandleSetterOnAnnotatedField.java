package ghIssues.issue2749;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * A MethodHandle is used only to write an annotated field. The write through the handle means the field is not assumed
 * to be injected, so it is reported as unread.
 */
public class WithMethodHandleSetterOnAnnotatedField {
    private static final MethodHandle SETTER;

    static {
        try {
            SETTER = MethodHandles.lookup().findSetter(WithMethodHandleSetterOnAnnotatedField.class, "reflectiveField", String.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Deprecated
    private String reflectiveField;

    public void setReflectiveField(String newField) {
        try {
            SETTER.invokeExact(this, newField);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}
