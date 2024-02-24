package privateFinalLocks;

public class BadMethodSynchronizationWithPublicStaticSynchronization {
    public static synchronized void doStuff() { // locking on the class, bug should be detected here
        System.out.println("Do some stuff");
    }
}