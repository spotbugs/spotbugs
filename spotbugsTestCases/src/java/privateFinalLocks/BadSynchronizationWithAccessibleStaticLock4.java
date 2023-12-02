package privateFinalLocks;

public class BadSynchronizationWithAccessibleStaticLock4 {
    private static Object baseLock = new Object();

    public static void doStuff() {
        synchronized (baseLock) {
            System.out.println("Do stuff");
        }
    }

    public void updateField() {
        baseLock = new Object();
    }
}
