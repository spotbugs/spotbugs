package privateFinalLocks;

public class UnsafeMixedMethodSynchronization {
    public static synchronized void doStuff() { // Locks on the object's monitor(intrinsic lock), bug should be detected here
        System.out.println("Do some stuff");
    }

    public static UnsafeMixedMethodSynchronization lookup(String name) {
        return null;
    }

}
