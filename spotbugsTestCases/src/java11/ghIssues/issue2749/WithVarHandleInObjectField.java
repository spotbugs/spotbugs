package ghIssues.issue2749;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;

/**
 * The VarHandle is kept in a field declared as Object, so the declared type of the accessor field is
 * wider than the type of the handle stored in it.
 */
public class WithVarHandleInObjectField {
    private static final Object VH;

    static {
        try {
            VH = MethodHandles.lookup().findVarHandle(WithVarHandleInObjectField.class, "value", int.class);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private volatile int value;

    public int getValue() {
        return (int) ((VarHandle) VH).getAcquire(this);
    }

    public void setValue(int newValue) {
        ((VarHandle) VH).setRelease(this, newValue);
    }
}
