package privateFinalLocks;

class UnsafeMethodSynchronizationWithClassExposingItSelf {
    public synchronized void doStuff() { // Locks on the object's monitor(intrinsic lock), bug should be detected here
        System.out.println("Do some stuff");
    }

    public synchronized void doStuff2() { // Locks on the object's monitor(intrinsic lock), bug should be detected here
        System.out.println("Do some stuff");
    }

    public static UnsafeMethodSynchronizationWithClassExposingItSelf lookup(String name) {
        return null;
    }

    public static UnsafeMethodSynchronizationWithClassExposingItSelf lookup2(String name) {
        return null;
    }
}
