package ghIssues.issue2749;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Create AtomicFieldUpdater, but invoke only setter.
 */
public class WithAtomicUpdaterGetterNotInvoked {
    public final class Value {
        // nothing else
    }

    private static final AtomicReferenceFieldUpdater<WithAtomicUpdaterGetterNotInvoked,
        Value> UPDATER = AtomicReferenceFieldUpdater.newUpdater(
        WithAtomicUpdaterGetterNotInvoked.class, Value.class, "reflectiveField");

    private volatile Value reflectiveField;

    public void setReflectiveField(Value newField) {
        try {
            UPDATER.lazySet(this, newField);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}