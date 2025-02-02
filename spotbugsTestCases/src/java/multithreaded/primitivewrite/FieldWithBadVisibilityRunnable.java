package multithreaded.primitivewrite;

public class FieldWithBadVisibilityRunnable implements Runnable {
    private boolean done = false;

    @Override
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
}
