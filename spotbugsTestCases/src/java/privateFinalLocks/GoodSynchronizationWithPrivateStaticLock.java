package privateFinalLocks;

public class GoodSynchronizationWithPrivateStaticLock {
    private static final Object lock = new Object(); // private final lock object

    public static void changeValue() {
        synchronized (lock) { // Locks on the private Object
            System.out.println("Change some value");
        }

    }
}
