package privateFinalLocks;

public class BadSynchronizationWithExposedLockToUntrustedCode {
    // the problem arises when a class extends the current class and can access the lock
    protected Object baseLock = new Object(); // bug should be detected here
    protected Object lock = new Object(); // bug should be detected here
    public void doSomeStuff() {
        synchronized (baseLock) { // synchronizing on a lock that can be accessed by descendants
            System.out.println("Do stuff");
        }
    }
}

class BadSynchronizationWithLockFromParent extends BadSynchronizationWithExposedLockToUntrustedCode {
    public void doSomeStuff2() {
        synchronized (lock) { // synchronizing on a lock used by a descendant
            System.out.println("Do stuff");
        }
    }
}
