package privateFinalLocks;

public class BadSynchronizationWithAccessibleStaticLock1 {
    public static Object baseLock = new Object();
    public static Object lock = new Object();

    public static void doStuff() {
        synchronized (baseLock) {
            System.out.println("Do stuff");
        }
    }
}

class BadSynchronizationWithAccessibleStaticLockFromParent1 extends BadSynchronizationWithAccessibleStaticLock1 {
    public static void doStuff2() {
        synchronized (lock) {
            System.out.println("Do stuff");
        }
    }
}
