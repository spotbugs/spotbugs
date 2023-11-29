package privateFinalLocks;

public class BadMethodSynchronizationWithPublicStaticLock {
    public static synchronized void changeValue() { // locking on the class, bug should be detected here
        System.out.println("Change some value");
    }
}