package ghIssues.issue2749;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * A VarHandle is used, but only to get the value. The field was never set.
 */
public class WithVarHandleNoWriting {
    public final class Value {
        // nothing else
    }

    private static final VarHandle VH;

    static {
        try {
            VH = MethodHandles.lookup().findVarHandle(WithVarHandleNoWriting.class, "value", Value.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Value value;

    public Value value() {
        var read = (Value) VH.getAcquire(this);
        return read;
    }
}