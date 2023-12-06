package privateFinalLocks;

public class BadSynchronizationWithAccessibleStaticLock4 {
    private static Object baseLock = new Object();

    public static void doStuff() {
        synchronized (baseLock) { // synchronizing on the accessible lock object, bug should be detected here
            System.out.println("Do stuff");
        }
    }

    public void updateField() {
        baseLock = new Object();
    }
}
