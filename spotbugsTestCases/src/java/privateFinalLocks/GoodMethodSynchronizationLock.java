package privateFinalLocks;

public class GoodMethodSynchronizationLock {
    public static void untrustedCode() {
        System.out.println("Do some untrusted stuff");
    }
}

class GoodSampleClass {
    public synchronized void changeValue() { // Locks on the object's monitor(intrinsic lock), bug should be detected here
        System.out.println("Change some value");
    }

    private static GoodSampleClass lookup(String name) { // exposing the lock object, bug should be detected here
        return null;
    }
}

class SomeOtherGoodSampleClass {
    public synchronized void changeValue() { // Locks on the object's monitor(intrinsic lock), bug should be detected here
        System.out.println("Change some value");
    }

    private static SomeOtherGoodSampleClass lookup(String name) { // exposing the lock object, bug should be detected here
        return null;
    }
}
