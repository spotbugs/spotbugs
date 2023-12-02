package privateFinalLocks;

public class BadSynchronizationWithVolatileLock3 {
    private volatile Object baseLock = new Object();
    protected volatile Object lock = new Object();

    public void doSomeStuff() {
        synchronized (baseLock) { // synchronizing on a lock in the presence of an accessor, bug should be detected here
            System.out.println("Do stuff");
        }
    }
    public void updateBaseLockWithLocalVariable() {
        Object newLockValue = new Object();
        baseLock = newLockValue;
    }
}

class BadSynchronizationWithVolatileLockFromParent3 extends BadSynchronizationWithVolatileLock3 {
    public void doSomeStuff2() {
        synchronized (lock) { // synchronizing on accessible lock object, bug should be detected here
            System.out.println("Do stuff");
        }
    }
    public void updateLockWithLocalVariable() {
        Object newLockValue = new Object();
        lock = newLockValue;
    }
}
