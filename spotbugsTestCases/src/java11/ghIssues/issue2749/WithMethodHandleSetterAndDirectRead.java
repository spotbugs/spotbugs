package ghIssues.issue2749;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * A MethodHandle is used only to write a field. The field is read directly.
 */
public class WithMethodHandleSetterAndDirectRead {
    public static final class Value {
        // nothing else
    }

    private static final MethodHandle SETTER;

    static {
        try {
            SETTER = MethodHandles.lookup().findSetter(WithMethodHandleSetterAndDirectRead.class, "reflectiveField", Value.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Value reflectiveField;

    public Value getReflectiveField() {
        return reflectiveField;
    }

    public void setReflectiveField(Value newField) {
        try {
            SETTER.invokeExact(this, newField);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}
