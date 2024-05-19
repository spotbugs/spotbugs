package privateFinalLocks;

public final class SafeSynchronizationWithProtectedLockInFinalClass {
    protected Object lock1 = new Object();
    protected final Object lock2 = new Object();
    protected static Object lock3 = new Object();
    protected volatile Object lock4 = new Object();

    public void doStuff1() {
        synchronized (lock1) {
            System.out.println("Do stuff");
        }
    }

    public void doStuff2() {
        synchronized (lock2) {
            System.out.println("Do stuff");
        }
    }

    public void doStuff3() {
        synchronized (lock3) {
            System.out.println("Do stuff");
        }
    }

    public void doStuff4() {
        synchronized (lock4) {
            System.out.println("Do stuff");
        }
    }

}
