package atomicMethods;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class UnsafeAtomicReference2 {

    private final AtomicBoolean atomicBoolean = new AtomicBoolean(true);
    private final AtomicBoolean atomicBoolean2 = new AtomicBoolean(true);

    private final AtomicInteger atomicInteger = new AtomicInteger(0);
    private final AtomicInteger atomicInteger2 = new AtomicInteger(0);

    public void incrementAtomicInteger() {
        atomicInteger.incrementAndGet();
        if (atomicBoolean.get()) {
            System.out.println(atomicInteger.get());
            if (atomicInteger.get() >= 100) atomicBoolean.set(false);
        }
    }

    public void incrementAtomicInteger2() {
        atomicInteger2.incrementAndGet();
        if (atomicBoolean2.get()) {
            System.out.println(atomicInteger2.get());
            if (atomicInteger2.get() >= 100) atomicBoolean2.set(false);
        }
    }
}
