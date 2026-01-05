package atomicMethods;

import java.util.concurrent.atomic.AtomicInteger;

public class UnsafeAtomicReferenceValueSet {

    private AtomicInteger atomicInteger;

    public void init() {
        atomicInteger = new AtomicInteger(0);
    }

    public void update() {
        atomicInteger.set(1);
        atomicInteger = new AtomicInteger(2);
    }
}
