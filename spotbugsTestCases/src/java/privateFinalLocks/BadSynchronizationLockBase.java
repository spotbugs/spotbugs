package privateFinalLocks;

public class BadSynchronizationLockBase {
    public final Object baseLock = new Object();
    protected final Object lock = new Object();

    public void doStuff() {
        synchronized (baseLock) {
            System.out.println("Do some stuff");
        }
    }
}

class BadSynchronizationWithLockFromBase extends BadSynchronizationLockBase {
    public void doOtherStuff() {
        synchronized (baseLock) {
            System.out.println("Do some other stuff");
        }
    }

    public void changeValue() {
        synchronized (lock) {
            System.out.println("Change some values");
        }
    }
}
