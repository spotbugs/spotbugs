package multithreaded.primitivewrite;

public class SynchronizedMethodAndBadVisibilityOnField {
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

    public synchronized void print() {
        System.out.println("synchronized method");
    }
}
