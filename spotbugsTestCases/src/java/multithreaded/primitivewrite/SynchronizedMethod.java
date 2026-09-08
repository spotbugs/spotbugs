package multithreaded.primitivewrite;

public class SynchronizedMethod {
    private boolean done = false;

    public synchronized void doSomething() {
        while (!done) {
            try {
                Thread.sleep(1000);
            } catch(InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public synchronized void shutdown() {
        done = true;
    }

}
