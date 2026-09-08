package multithreaded.primitivewrite;

public class SynchronizedSeparateMethod implements Runnable {
    private boolean done = false;

    @Override public void run() {
        while (!isDone()) {
            try {
                // ...
                Thread.sleep(1000); // Do something
            } catch(InterruptedException ie) {
                Thread.currentThread().interrupt(); // Reset interrupted status
            }
        }
    }

    public synchronized boolean isDone() {
        return done;
    }

    public synchronized void shutdown() {
        done = true;
    }
}
