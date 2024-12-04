package multithreaded.primitivewrite;

public class FieldWithBadVisibilityReordered extends Thread {
    private boolean done = false;

    public void shutdown() {
        done = true;
    }

    @Override
    public void run() {
        while (!done) {
            try {
                sleep(1000);
            } catch(InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
