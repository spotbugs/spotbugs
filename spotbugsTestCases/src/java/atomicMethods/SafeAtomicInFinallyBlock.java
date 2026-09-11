package atomicMethods;

import java.util.concurrent.atomic.AtomicInteger;

public class SafeAtomicInFinallyBlock {

    private final AtomicInteger atomicInteger;

    public SafeAtomicInFinallyBlock() {
        atomicInteger = new AtomicInteger();
    }

    public void sleepAndDecrement() {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            atomicInteger.decrementAndGet();
        }
    }
}
