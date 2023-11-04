package privateFinalLocks;

public class GoodMethodSynchronizationLock {
    public static void untrustedCode() {
        System.out.println("Do some untrusted stuff");
    }
}

class GoodSampleClass {
    public synchronized void changeValue() {
        System.out.println("Change some value");
    } // Locks on the object's monitor(intrinsic lock)

    private static GoodSampleClass lookup(String name) { // exposing the lock object, bug should be detected here
        return null;
    }
}

class SomeOtherGoodSampleClass {
    public synchronized void changeValue() {
        System.out.println("Change some value");
    } // Locks on the object's monitor(intrinsic lock)

    private static SomeOtherGoodSampleClass lookup(String name) { // exposing the lock object, bug should be detected here
        return null;
    }
}
