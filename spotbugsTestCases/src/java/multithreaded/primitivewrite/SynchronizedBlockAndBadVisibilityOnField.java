package multithreaded.primitivewrite;

public class SynchronizedBlockAndBadVisibilityOnField {
    private boolean done = false;

    public void run() {
        while (!done) {
            try {
                Thread.sleep(1000);
            } catch(InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void shutdown() {
        done = true;
    }

    public void print() {
        synchronized (SynchronizedBlockAndBadVisibilityOnField.class) {
            System.out.println("synchronized block");
        }
    }
}
