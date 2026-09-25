package synchronizationLocks.privateFinalLocks;

public class UnsafeSynchronizationNewObjectTarget {

    private Object lock = new Object();

    public void doStuff() {
        synchronized (lock) {
            System.out.println("Do stuff");
        }
    }

    public static UnsafeSynchronizationNewObjectTarget create() {
        UnsafeSynchronizationNewObjectTarget f = new UnsafeSynchronizationNewObjectTarget();
        f.lock = new Object();
        return f;
    }
}