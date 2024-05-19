package privateFinalLocks;

public class UnsafeMethodSynchronizationWithPublicStaticSynchronization {
    public static synchronized void doStuff() { // locking on the class, bug should be detected here
        System.out.println("Do some stuff");
    }
}