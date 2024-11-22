package multithreaded.sharedPrimitiveVariables;

public class NonsynchronizedSeparateMethod implements Runnable {
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
        return done;
    }

    public void shutdown() {
        done = true;
    }
}
