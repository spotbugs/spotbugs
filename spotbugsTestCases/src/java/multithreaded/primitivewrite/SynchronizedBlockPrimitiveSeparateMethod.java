package multithreaded.primitivewrite;

public class SynchronizedBlockPrimitiveSeparateMethod implements Runnable {
    private boolean done = false;

    @Override
    public void run() {
        synchronized (SynchronizedBlockPrimitiveSeparateMethod.class) {
            while (!isDone()) {
                try {
                    // ...
                    Thread.sleep(1000); // Do something
                } catch(InterruptedException ie) {
                    Thread.currentThread().interrupt(); // Reset interrupted status
                }
            }
        }
    }

    private boolean isDone() {
        return done;
    }

    public void shutdown() {
        synchronized (SynchronizedBlockPrimitiveSeparateMethod.class) {
            done = true;
        }
    }
}
