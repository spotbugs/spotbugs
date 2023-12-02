package privateFinalLocks;

public class BadSynchronizationWithVolatileLock1 {
    private volatile Object baseLock = new Object();
    protected volatile Object lock = new Object();

    public void doSomeStuff() {
        synchronized (baseLock) { // synchronizing on a lock in the presence of an accessor, bug should be detected here
            System.out.println("Do stuff");
        }
    }
    public void updateBaseLock() {
        lock = new Object();
    }
}

class BadSynchronizationWithVolatileLockFromParent1 extends BadSynchronizationWithVolatileLock1 {
    public void doSomeStuff2() {
        synchronized (lock) { // synchronizing on accessible lock object, bug should be detected here
            System.out.println("Do stuff");
        }
    }
    public void updateLock() {
        lock = new Object();
    }
}
