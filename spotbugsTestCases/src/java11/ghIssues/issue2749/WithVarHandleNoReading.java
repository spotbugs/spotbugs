package ghIssues.issue2749;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * A VarHandle is used, but only to set the value. The field was never read.
 */
public class WithVarHandleNoReading {
    public final class Value {
        // nothing else
    }

    private static final VarHandle VH;

    static {
        try {
            VH = MethodHandles.lookup().findVarHandle(WithVarHandleNoReading.class, "value", Value.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Value value;

    private void computeValue() {
        var computed = new Value();
        VH.setRelease(this, computed);
    }
}