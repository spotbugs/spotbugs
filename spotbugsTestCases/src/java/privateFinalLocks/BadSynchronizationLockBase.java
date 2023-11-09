package privateFinalLocks;

public class BadSynchronizationLockBase {
    public final Object baseLock = new Object();
    protected final Object lock = new Object();

    public void doStuff() {
        synchronized (baseLock) { // synchronizing on the public lock object, bug should be detected here
            System.out.println("Do some stuff");
        }
    }
}

class BadSynchronizationWithLockFromBase extends BadSynchronizationLockBase {
    public void doOtherStuff() {
        synchronized (baseLock) { // synchronizing on the public lock object inherited from parent, bug should be detected here
            System.out.println("Do some other stuff");
        }
    }

    public void changeValue() {
        synchronized (lock) { // synchronizing on the lock object inherited from parent, bug should be detected here
            System.out.println("Change some values");
        }
    }
}
