package multithreaded.sharedPrimitiveVariables;

public class SynchronizedBlockWithBadVisibilityOnField {
    private boolean done = false;

    public void run() {
        synchronized (SynchronizedBlockWithBadVisibilityOnField.class) {
            System.out.println("this is synchronized, but not the whole method");
        }
        while (!done) {
            try {
                Thread.currentThread().sleep(1000);
            } catch(InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void shutdown() {
        done = true;
    }
}
