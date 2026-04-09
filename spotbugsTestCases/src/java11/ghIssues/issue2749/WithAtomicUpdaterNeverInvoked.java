package ghIssues.issue2749;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

/**
 * Create AtomicFieldUpdater, but never invoke it.
 */
public class WithAtomicUpdaterNeverInvoked {
    public final class Value {
        // nothing else
    }

    private static final AtomicReferenceFieldUpdater<WithAtomicUpdaterNeverInvoked, Value> UPDATER =
            AtomicReferenceFieldUpdater.newUpdater(WithAtomicUpdaterNeverInvoked.class, Value.class, "reflectiveField");

    private volatile Value reflectiveField;
}
