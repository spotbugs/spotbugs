package privateFinalLocks;

public class BadSynchronizationWithVolatileLock2 {
    private volatile Object baseLock = new Object();
    protected volatile Object lock = new Object();

    public void doSomeStuff() {
        synchronized (baseLock) { // synchronizing on a lock in the presence of an accessor, bug should be detected here
            System.out.println("Do stuff");
        }
    }
    public void updateBaseLockWithParam(Object newLockValue) {
        baseLock = newLockValue;
    }
}

class BadSynchronizationWithVolatileLockFromParent2 extends BadSynchronizationWithVolatileLock2 {
    public void doSomeStuff2() {
        synchronized (lock) { // synchronizing on accessible lock object, bug should be detected here
            System.out.println("Do stuff");
        }
    }
    public void updateLockWithParam(Object newLockValue) {
        lock = newLockValue;
    }
}
