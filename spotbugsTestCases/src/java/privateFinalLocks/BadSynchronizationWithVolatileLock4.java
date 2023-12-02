package privateFinalLocks;

public class BadSynchronizationWithVolatileLock4 {
    private volatile Object baseLock = new Object();
    protected volatile Object lock = new Object();

    public void doSomeStuff() {
        synchronized (baseLock) { // synchronizing on a lock in the presence of an exposing method, bug should be detected here
            System.out.println("Do stuff");
        }
    }
    public Object getBaseLock() {
        return baseLock ;
    }
}

class BadSynchronizationWithVolatileLockFromParent4 extends BadSynchronizationWithVolatileLock4 {
    public void doSomeStuff2() {
        synchronized (lock) { // synchronizing on a lock in the presence of an exposing method, bug should be detected here
            System.out.println("Do stuff");
        }
    }
    public Object getLock() {
        return lock ;
    }
}
