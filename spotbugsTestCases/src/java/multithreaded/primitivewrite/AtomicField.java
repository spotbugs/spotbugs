package multithreaded.primitivewrite;

import java.util.concurrent.atomic.AtomicBoolean;

public class AtomicField {
    private AtomicBoolean done = new AtomicBoolean(false);

    public void doSomething() {
        while (!done.get()) {
            try {
                Thread.sleep(1000);
            } catch(InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void shutdown() {
        done.set(true);
    }
}
