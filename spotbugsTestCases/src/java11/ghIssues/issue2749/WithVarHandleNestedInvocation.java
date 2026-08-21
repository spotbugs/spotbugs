package ghIssues.issue2749;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * One VarHandle invocation is nested inside another one, so the accessor of the inner call is not
 * the deepest item on the operand stack.
 */
public class WithVarHandleNestedInvocation {
    public final class Value {
        // nothing else
    }

    private static final VarHandle SOURCE_VH;
    private static final VarHandle TARGET_VH;

    static {
        try {
            SOURCE_VH = MethodHandles.lookup().findVarHandle(WithVarHandleNestedInvocation.class, "source", Value.class);
            TARGET_VH = MethodHandles.lookup().findVarHandle(WithVarHandleNestedInvocation.class, "target", Value.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private volatile Value source;
    private volatile Value target;

    public void copy() {
        TARGET_VH.set(this, (Value) SOURCE_VH.get(this));
    }
}
