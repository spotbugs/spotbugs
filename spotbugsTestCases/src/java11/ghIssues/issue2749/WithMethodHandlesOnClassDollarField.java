package ghIssues.issue2749;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * Two separate MethodHandles are created, but only the setter is used. The field name contains "class$", like the
 * fields older compilers generated for class literals, so the field is excluded from reporting, as in the ordinary
 * analysis.
 */
public class WithMethodHandlesOnClassDollarField {
    private static final MethodHandle GETTER;
    private static final MethodHandle SETTER;

    static {
        var lookup = MethodHandles.lookup();
        try {
            GETTER = lookup.findGetter(WithMethodHandlesOnClassDollarField.class, "class$reflectiveField", String.class);
            SETTER = lookup.findSetter(WithMethodHandlesOnClassDollarField.class, "class$reflectiveField", String.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private String class$reflectiveField;

    public void setReflectiveField(String newField) {
        try {
            SETTER.invokeExact(this, newField);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}
