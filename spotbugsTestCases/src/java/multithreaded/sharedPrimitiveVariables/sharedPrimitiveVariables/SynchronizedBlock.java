package multithreaded.sharedPrimitiveVariables.sharedPrimitiveVariables;

public class SynchronizedBlock implements Runnable {
    private boolean done = false;

    @Override
    public void run() {
        synchronized (SynchronizedBlock.class) {
            while (!done) {
                try {
                    Thread.currentThread().sleep(1000);
                } catch(InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    public void shutdown() {
        done = true;
    }
}
