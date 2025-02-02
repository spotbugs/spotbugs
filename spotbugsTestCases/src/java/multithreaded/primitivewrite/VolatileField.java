package multithreaded.primitivewrite;

public class VolatileField {
    private volatile boolean done = false;

    public void doSomething() {
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
