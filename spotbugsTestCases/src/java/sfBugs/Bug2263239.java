package sfBugs;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

public class Bug2263239 {
    volatile Object[] _vol;

    Object falsePositive() {

        Object[] a = _vol;
        if (a != null)
            return a;

        Object[] b = _vol;
        if (b != null)
            return b;

        return "foo";
    }

    private final AtomicReferenceFieldUpdater<Bug2263239, Object[]> _newUpdater = AtomicReferenceFieldUpdater.newUpdater(
            Bug2263239.class, Object[].class, "_vol");

    void cas(Object[] expect, Object[] update) {
        _newUpdater.compareAndSet(this, expect, update);
    }

}
