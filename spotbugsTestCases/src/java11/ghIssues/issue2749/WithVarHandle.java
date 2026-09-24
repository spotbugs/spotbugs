package ghIssues.issue2749;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * A VarHandle is used to perform lazy instantiation.
 */
public class WithVarHandle {
    public final class Value {
        // nothing else
    }

    private static final VarHandle VH;

    static {
        try {
            VH = MethodHandles.lookup().findVarHandle(WithVarHandle.class, "value", Value.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Value value;

    public Value value() {
        var read = (Value) VH.getAcquire(this);
        return read != null ? read : computeValue();
    }

    private Value computeValue() {
        var computed = new Value();
        var witness = (Value) VH.compareAndExchangeRelease(this, null, computed);
        return witness != null ? witness : computed;
    }
}