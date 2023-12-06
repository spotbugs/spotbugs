package privateFinalLocks;

public class BadSynchronizationWithAccessibleStaticLock2 {
    protected static Object baseLock = new Object();
    protected static Object lock = new Object();

    public static void doStuff() {
        synchronized (baseLock) {
            System.out.println("Do stuff");
        }
    }
}

class BadSynchronizationWithAccessibleStaticLockFromParent2 extends BadSynchronizationWithAccessibleStaticLock2 {
    public static void doStuff2() {
        synchronized (lock) { // synchronizing on the protected lock object, bug should be detected here
            System.out.println("Do stuff");
        }
    }
}