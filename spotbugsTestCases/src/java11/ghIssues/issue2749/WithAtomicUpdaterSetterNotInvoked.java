package ghIssues.issue2749;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Create AtomicFieldUpdater, but invoke only getter.
 */
public class WithAtomicUpdaterSetterNotInvoked {
    public final class Value {
        // nothing else
    }

    private static final AtomicReferenceFieldUpdater<WithAtomicUpdaterSetterNotInvoked,
        Value> UPDATER = AtomicReferenceFieldUpdater.newUpdater(
        WithAtomicUpdaterSetterNotInvoked.class, Value.class, "reflectiveField");

    private volatile Value reflectiveField;

    public Value getReflectiveField() {
        try {
            Value reflectiveFieldValue = (Value) UPDATER.get(this);
            return reflectiveFieldValue;
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}