package synchronizationLocks.privateFinalLocks;

public class BadMixedMethodSynchronization {
    public static synchronized void doStuff() { // Locks on the object's monitor(intrinsic lock), bug should be detected here
        System.out.println("Do some stuff");
    }

    public static BadMixedMethodSynchronization lookup(String name) {
        return null;
    }

}
