package privateFinalLocks;

public class BadSynchronizationWithPublicFinalLock {
    /*
        @note: Public final locks are already handled by rule [https://wiki.sei.cmu.edu/confluence/display/java/OBJ01-J.+Limit+accessibility+of+fields]
        Although it should be reported when used in problematic synchronization too
     */
    public final Object baseLock = new Object();
    protected final Object lock = new Object();

    public void doSomeStuff() {
        synchronized (baseLock) { // synchronizing on the public lock object, bug should be detected here
            System.out.println("Do stuff");
        }
    }
}

class BadSynchronizationWithPublicFinalLockFromParent extends BadSynchronizationWithPublicFinalLock {
    public void doSomeStuff2() {
        synchronized (baseLock) { // synchronizing on the public lock object inherited from parent, bug should be detected here
            System.out.println("Do stuff");
        }
    }

    public void doSomeStuff3() {
        synchronized (lock) { // synchronizing on the protected lock object inherited from parent, bug should be detected here
            System.out.println("Do stuff");
        }
    }
}

