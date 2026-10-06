package ghIssues.issue2749;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * A VarHandle is created, but never invoked
 */
public class WithVarHandleNeverInvoked {
    public final class Value {
        // nothing else
    }

    private static final VarHandle VH;

    static {
        try {
            VH = MethodHandles.lookup().findVarHandle(WithVarHandleNeverInvoked.class, "value", Value.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private Value value;
}