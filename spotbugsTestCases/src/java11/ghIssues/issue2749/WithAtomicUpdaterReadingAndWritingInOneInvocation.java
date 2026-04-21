package ghIssues.issue2749;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Create AtomicFieldUpdater and read/write in one invocation
 */
public class WithAtomicUpdaterReadingAndWritingInOneInvocation {
    public final class Value {
        // nothing else
    }

    private static final AtomicReferenceFieldUpdater<WithAtomicUpdaterReadingAndWritingInOneInvocation,
        Value> UPDATER = AtomicReferenceFieldUpdater.newUpdater(
        WithAtomicUpdaterReadingAndWritingInOneInvocation.class, Value.class, "reflectiveField");

    private volatile Value reflectiveField;

    public void setReflectiveField(Value newField) {
        try {
            UPDATER.compareAndSet(this, null, newField);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}