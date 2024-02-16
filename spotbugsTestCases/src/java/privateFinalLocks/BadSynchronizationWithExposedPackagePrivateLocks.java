package privateFinalLocks;

public class BadSynchronizationWithExposedPackagePrivateLocks {
    Object lock1 = new Object();
    final Object lock2 = new Object();
    static Object lock3 = new Object();
    volatile Object lock4 = new Object();

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
