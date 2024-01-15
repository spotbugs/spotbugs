package privateFinalLocks;

public final class GoodSynchronizationWithProtectedLockInFinalClass {
    protected Object lock = new Object(); // protected final lock object
    protected final Object lock2 = new Object(); // protected final lock object

    public void changeValue() {
        synchronized (lock) { // Locks on the protected Object
            System.out.println("Change some value");
        }
    }

    public void changeValue2() {
        synchronized (lock2) { // Locks on the protected Object
            System.out.println("Change some value");
        }
    }
}
