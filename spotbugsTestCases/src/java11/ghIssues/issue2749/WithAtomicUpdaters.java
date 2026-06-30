package ghIssues.issue2749;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class WithAtomicUpdaters {
    public final class Value {
        // nothing else
    }

    private static final AtomicReferenceFieldUpdater<WithAtomicUpdaters, Value> REFERENCE_UPDATER =
        AtomicReferenceFieldUpdater.newUpdater(WithAtomicUpdaters.class, Value.class, "reflectiveObjectField");

    private static final AtomicIntegerFieldUpdater<WithAtomicUpdaters> INT_UPDATER =
        AtomicIntegerFieldUpdater.newUpdater(WithAtomicUpdaters.class, "reflectiveIntField");

    private static final AtomicLongFieldUpdater<WithAtomicUpdaters> LONG_UPDATER =
        AtomicLongFieldUpdater.newUpdater(WithAtomicUpdaters.class, "reflectiveLongField");


    private volatile Value reflectiveObjectField;
    private volatile int reflectiveIntField;
    private volatile long reflectiveLongField;

    public void setReflectiveFields(Value newObject, int newInt, long newLong) {
        try {
            REFERENCE_UPDATER.weakCompareAndSet(this, null, newObject);
            LONG_UPDATER.getAndSet(this, newLong);
            INT_UPDATER.getAndIncrement(this);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}