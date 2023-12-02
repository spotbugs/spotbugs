package privateFinalLocks;

public class BadSynchronizationWithVolatileLock6 { // @note: this is hard, I don't know how to solve this
    protected volatile Object baseLock = new Object();

    public void doSomeStuff() {
        synchronized (baseLock) { // synchronizing on a lock in the presence of an accessor in a descendant, bug should be detected here
            System.out.println("Do stuff");
        }
    }
}

class BadSynchronizationWithVolatileLockFromParent6 extends BadSynchronizationWithVolatileLock6 {
    public void updateBaseLock() {
        baseLock = new Object();
    }
}
