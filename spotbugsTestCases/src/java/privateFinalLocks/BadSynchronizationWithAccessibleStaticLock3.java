package privateFinalLocks;

public class BadSynchronizationWithAccessibleStaticLock3 {
    static Object baseLock = new Object();
    static Object lock = new Object();

    public static void doStuff() {
        synchronized (baseLock) {
            System.out.println("Do stuff");
        }
    }
}

class BadSynchronizationWithAccessibleStaticLockFromParent3 extends BadSynchronizationWithAccessibleStaticLock3 {
    public static void doStuff2() {
        synchronized (lock) {
            System.out.println("Do stuff");
        }
    }
}