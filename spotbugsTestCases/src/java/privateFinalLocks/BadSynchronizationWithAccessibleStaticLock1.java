package privateFinalLocks;

public class BadSynchronizationWithAccessibleStaticLock1 {
    public static Object baseLock = new Object();
    public static Object lock = new Object();

    public static void doStuff() {
        synchronized (baseLock) {
            System.out.println("Do stuff");
        }
    }

    /**
     * @todo: Extend test case here:
     *          by adding an accessor/getter: it should only detect one BUG, probably the public lock
     */
}

class BadSynchronizationWithAccessibleStaticLockFromParent1 extends BadSynchronizationWithAccessibleStaticLock1 {
    public static void doStuff2() {
        synchronized (lock) { // synchronizing on the public static lock object, bug should be detected here
            System.out.println("Do stuff");
        }
    }
}
