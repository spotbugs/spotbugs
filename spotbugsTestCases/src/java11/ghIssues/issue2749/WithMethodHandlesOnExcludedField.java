package ghIssues.issue2749;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * Two separate MethodHandles are created, but only the getter is used. The field name starts with an underscore, so
 * the field is excluded from reporting, as in the ordinary analysis.
 */
public class WithMethodHandlesOnExcludedField {
    private static final MethodHandle GETTER;
    private static final MethodHandle SETTER;

    static {
        var lookup = MethodHandles.lookup();
        try {
            GETTER = lookup.findGetter(WithMethodHandlesOnExcludedField.class, "_reflectiveField", String.class);
            SETTER = lookup.findSetter(WithMethodHandlesOnExcludedField.class, "_reflectiveField", String.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private String _reflectiveField;

    public String getReflectiveField() {
        try {
            return (String) GETTER.invokeExact(this);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}
