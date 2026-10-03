package ghIssues.issue2749;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * A VarHandle is used only to read a static field. The field is written directly by its initializer.
 */
public class WithStaticVarHandleInitialized {
    public static final class Value {
        // nothing else
    }

    private static Value value = new Value();

    private static final VarHandle VH;

    static {
        try {
            VH = MethodHandles.lookup().findStaticVarHandle(WithStaticVarHandleInitialized.class, "value", Value.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public static Value value() {
        return (Value) VH.getAcquire();
    }
}
