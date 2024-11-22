package multithreaded.sharedPrimitiveVariables;

public class FieldWithBadVisibilityReordered extends Thread {
    private boolean done = false;

    public void shutdown() {
        done = true;
    }

    @Override
    public void run() {
        while (!done) {
            try {
                Thread.currentThread().sleep(1000);
            } catch(InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
