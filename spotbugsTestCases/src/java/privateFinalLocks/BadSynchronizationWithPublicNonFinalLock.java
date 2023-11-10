package privateFinalLocks;

public class BadSynchronizationWithPublicNonFinalLock {
    public Object baseLock = new Object();
    protected Object lock = new Object();

    public void doSomeStuff() {
        synchronized (baseLock) { // synchronizing on publicly accessible lock object, bug should be detected here
            stuff();
        }
    }

    public void stuff() {
        int x = 2;
        System.out.println(x * 5);
    }
}

class BadSynchronizationWithNonFinalLockFromParent extends BadSynchronizationWithPublicNonFinalLock {
    public void doSomeStuff2() {
        synchronized (baseLock) { // synchronizing on publicly accessible lock object inherited from parent, bug should be detected here
            stuff();
        }
    }

    public void doSomeStuff3() {
        synchronized (lock) { // synchronizing on shared lock object with parent, bug should be detected here
            stuff();
        }
    }
}
