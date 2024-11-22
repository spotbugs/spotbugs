package multithreaded.sharedPrimitiveVariables;

public class SynchronizedBlockSeparateMethod implements Runnable {
    private boolean done = false;

    @Override public void run() {
        while (!isDone()) {
            try {
                // ...
                Thread.currentThread().sleep(1000); // Do something
            } catch(InterruptedException ie) {
                Thread.currentThread().interrupt(); // Reset interrupted status
            }
        }
    }

    public boolean isDone() {
        synchronized (SynchronizedBlockSeparateMethod.class) {
            return done;
        }
    }

    public void shutdown() {
        synchronized (SynchronizedBlockSeparateMethod.class) {
            done = true;
        }
    }
}
