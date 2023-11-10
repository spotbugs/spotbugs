package privateFinalLocks;

public class BadSynchronizationWithPublicFinalLock {
    public final Object baseLock = new Object();
    protected final Object lock = new Object();

    public void doSomeStuff() {
        synchronized (baseLock) { // synchronizing on the public lock object, bug should be detected here
            stuff();
        }
    }

    public void stuff() {
        int x = 2;
        System.out.println(x * 5);
    }
}

class BadSynchronizationWithPublicFinalLockFromParent extends BadSynchronizationWithPublicFinalLock {
    public void doSomeStuff2() {
        synchronized (baseLock) { // synchronizing on the public lock object inherited from parent, bug should be detected here
            stuff();
        }
    }

    public void doSomeStuff3() {
        synchronized (lock) { // synchronizing on the protected lock object inherited from parent, bug should be detected here
            stuff();
        }
    }
}

