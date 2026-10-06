package ghIssues.issue2749;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * A MethodHandle is used only to read a field of a class with a native method. Native code may write the field, so it
 * is not reported as unwritten.
 */
public class WithMethodHandleGetterInNativeClass {
    private static final MethodHandle GETTER;

    static {
        try {
            GETTER = MethodHandles.lookup().findGetter(WithMethodHandleGetterInNativeClass.class, "reflectiveField", String.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private String reflectiveField;

    public native void initReflectiveField();

    public String getReflectiveField() {
        try {
            return (String) GETTER.invokeExact(this);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}
