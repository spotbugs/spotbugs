package ghIssues.issue2749;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicLongFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * All three kinds of AtomicFieldUpdater are kept in fields declared as Object, so the declared type of
 * the accessor fields is wider than the type of the updaters stored in them.
 */
public class WithAtomicUpdatersInObjectFields {
    public final class Value {
        // nothing else
    }

    private static final Object REFERENCE_UPDATER = AtomicReferenceFieldUpdater
        .newUpdater(WithAtomicUpdatersInObjectFields.class, Value.class, "reflectiveObjectField");

    private static final Object INT_UPDATER = AtomicIntegerFieldUpdater
        .newUpdater(WithAtomicUpdatersInObjectFields.class, "reflectiveIntField");

    private static final Object LONG_UPDATER = AtomicLongFieldUpdater
        .newUpdater(WithAtomicUpdatersInObjectFields.class, "reflectiveLongField");

    private volatile Value reflectiveObjectField;
    private volatile int reflectiveIntField;
    private volatile long reflectiveLongField;

    @SuppressWarnings("unchecked")
    public void setReflectiveFields(Value newObject, int newInt, long newLong) {
        try {
            ((AtomicReferenceFieldUpdater<WithAtomicUpdatersInObjectFields, Value>) REFERENCE_UPDATER)
                .weakCompareAndSet(this, null, newObject);
            ((AtomicLongFieldUpdater<WithAtomicUpdatersInObjectFields>) LONG_UPDATER).getAndSet(this, newLong);
            ((AtomicIntegerFieldUpdater<WithAtomicUpdatersInObjectFields>) INT_UPDATER).getAndIncrement(this);
        } catch (Throwable e) {
            throw new IllegalStateException(e);
        }
    }
}
