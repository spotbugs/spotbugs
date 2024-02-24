package privateFinalLocks;

public class GoodSynchronizationWithPrivateFinalLock {
    private final Object lock1 = new Object(); // private final lock object
    private static final Object lock2 = new Object(); // private final lock object

    public void doStuff1() {
        synchronized (lock1) { // Locks on the private Object
                System.out.println("Do some real stuff");
        }
    }

    public static void doStuff2() {
        synchronized (lock2) { // Locks on the private Object
            System.out.println("Do some real stuff");
        }

    }
}
