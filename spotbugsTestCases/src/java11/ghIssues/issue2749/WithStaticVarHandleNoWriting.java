package ghIssues.issue2749;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * A VarHandle is used, but only to get the value of a static field. The field was never set.
 */
public class WithStaticVarHandleNoWriting {
    public static final class Value {
        // nothing else
    }

    private static final VarHandle VH;

    static {
        try {
            VH = MethodHandles.lookup().findStaticVarHandle(WithStaticVarHandleNoWriting.class, "value", Value.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static Value value;

    public static Value value() {
        var read = (Value) VH.getAcquire();
        return read;
    }
}
