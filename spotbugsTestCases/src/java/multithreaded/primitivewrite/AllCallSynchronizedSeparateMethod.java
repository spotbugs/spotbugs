package multithreaded.primitivewrite;

public class AllCallSynchronizedSeparateMethod implements Runnable {
    private boolean done = false;

    @Override
    public void run() {
        synchronized (AllCallSynchronizedSeparateMethod.class) {
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

    private void shutdown() {
        done = true;
    }

    public synchronized void synchronizedShutdown() {
        shutdown();
    }
}
