package privateFinalLocks;

public class BadSynchronizationWithVolatileLock5 {
    protected volatile Object baseLock = new Object();
}

class BadSynchronizationWithVolatileLockFromParent5 extends BadSynchronizationWithVolatileLock5 {
    public void doSomeStuff() {
        synchronized (baseLock) { // synchronizing on a lock in the presence of an accessor in a descendant, bug should be detected here
            System.out.println("Do stuff");
        }
    }
    public void updateBaseLock() {
        baseLock = new Object();
    }
}
