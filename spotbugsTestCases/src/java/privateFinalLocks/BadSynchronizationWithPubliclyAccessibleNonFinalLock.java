package privateFinalLocks;

public class BadSynchronizationWithPubliclyAccessibleNonFinalLock {
    private volatile Object baseLock = new Object();
    protected volatile Object lock = new Object();

    public void doSomeStuff() {
        synchronized (baseLock) { // synchronizing on accessible lock object, bug should be detected here
            doSomeStuff();
        }
    }

    public void stuff() {
        int x = 2;
        System.out.println(x * 5);
    }
}

class BadSynchronizationWithPubliclyAccessibleNonFinalLockFromParent extends BadSynchronizationWithPubliclyAccessibleNonFinalLock {
    public void doSomeStuff2() {
        synchronized (lock) { // synchronizing on accessible lock object, bug should be detected here
            doSomeStuff();
        }
    }
}
