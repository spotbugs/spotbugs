package ghIssues.issue2749;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * A VarHandle is used to perform lazy instantiation of a static field.
 */
public class WithStaticVarHandle {
    public static final class Value {
        // nothing else
    }

    private static final VarHandle VH;

    static {
        try {
            VH = MethodHandles.lookup().findStaticVarHandle(WithStaticVarHandle.class, "value", Value.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private static Value value;

    public static Value value() {
        var read = (Value) VH.getAcquire();
        return read != null ? read : computeValue();
    }

    private static Value computeValue() {
        var computed = new Value();
        var witness = (Value) VH.compareAndExchangeRelease(null, computed);
        return witness != null ? witness : computed;
    }
}
