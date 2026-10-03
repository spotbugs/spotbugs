package ghIssues.issue2749;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * A MethodHandle is used only to write a field. The field name starts with an underscore, so the field is excluded
 * from reporting, as in the ordinary analysis.
 */
public class WithMethodHandleSetterOnExcludedField {
    private static final MethodHandle SETTER;

    static {
        try {
            SETTER = MethodHandles.lookup().findSetter(WithMethodHandleSetterOnExcludedField.class, "_reflectiveField", String.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private String _reflectiveField;

    public void setReflectiveField(String newField) {
        try {
            SETTER.invokeExact(this, newField);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}
