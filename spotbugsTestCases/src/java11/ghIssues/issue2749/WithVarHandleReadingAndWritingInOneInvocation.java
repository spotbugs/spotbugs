package ghIssues.issue2749;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * A VarHandle is invoked only once - both to get and set the value
 */
public class WithVarHandleReadingAndWritingInOneInvocation {
    public final class Value {
        // nothing else
    }

    private static final VarHandle VH;

    static {
        try {
            VH = MethodHandles.lookup().findVarHandle(WithVarHandleReadingAndWritingInOneInvocation.class, "value", Value.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Value value;

    private Value computeValue() {
        var computed = new Value();
        var witness = (Value) VH.compareAndExchangeRelease(this, null, computed);
        return witness != null ? witness : computed;
    }
}